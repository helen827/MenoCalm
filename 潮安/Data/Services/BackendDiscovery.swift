import Foundation
import Darwin

enum BackendDiscovery {
    private static let port: UInt16 = 32145
    private static let request = "CHAOAN_DISCOVERY_REQUEST"
    private static let responsePrefix = "CHAOAN_DISCOVERY_RESPONSE "

    static func discover(timeoutSeconds: TimeInterval = 1.2) -> URL? {
        if let url = discoverViaBroadcast(timeoutSeconds: timeoutSeconds) {
            return url
        }
        return discoverBySubnetScan(timeoutSeconds: min(1.0, timeoutSeconds))
    }

    private static func discoverViaBroadcast(timeoutSeconds: TimeInterval) -> URL? {
        let sock = socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP)
        guard sock >= 0 else { return nil }
        defer { close(sock) }

        var on: Int32 = 1
        _ = setsockopt(sock, SOL_SOCKET, SO_BROADCAST, &on, socklen_t(MemoryLayout.size(ofValue: on)))

        let sec = __darwin_time_t(timeoutSeconds)
        let usec = __darwin_suseconds_t((timeoutSeconds.truncatingRemainder(dividingBy: 1)) * 1_000_000)
        var tv = timeval(tv_sec: sec, tv_usec: usec)
        _ = setsockopt(sock, SOL_SOCKET, SO_RCVTIMEO, &tv, socklen_t(MemoryLayout.size(ofValue: tv)))

        var addr = sockaddr_in()
        addr.sin_len = UInt8(MemoryLayout<sockaddr_in>.size)
        addr.sin_family = sa_family_t(AF_INET)
        addr.sin_port = port.bigEndian
        addr.sin_addr = in_addr(s_addr: inet_addr("255.255.255.255"))

        let payload = Array(request.utf8)
        let sent: ssize_t = payload.withUnsafeBytes { ptr in
            var a = addr
            return withUnsafePointer(to: &a) {
                $0.withMemoryRebound(to: sockaddr.self, capacity: 1) { sa in
                    sendto(sock, ptr.baseAddress, ptr.count, 0, sa, socklen_t(MemoryLayout<sockaddr_in>.size))
                }
            }
        }
        guard sent > 0 else { return nil }

        var buffer = [UInt8](repeating: 0, count: 512)
        var from = sockaddr_in()
        var fromLen: socklen_t = socklen_t(MemoryLayout<sockaddr_in>.size)
        let received = withUnsafeMutablePointer(to: &from) { fromPtr in
            fromPtr.withMemoryRebound(to: sockaddr.self, capacity: 1) { sa in
                recvfrom(sock, &buffer, buffer.count, 0, sa, &fromLen)
            }
        }
        guard received > 0 else { return nil }
        let text = String(bytes: buffer.prefix(Int(received)), encoding: .utf8) ?? ""
        guard text.hasPrefix(responsePrefix) else { return nil }
        let urlText = text.dropFirst(responsePrefix.count).trimmingCharacters(in: .whitespacesAndNewlines)
        return URL(string: String(urlText))
    }

    private static func discoverBySubnetScan(timeoutSeconds: TimeInterval) -> URL? {
        guard let info = LocalNetworkInfo.wifiIpv4() else { return nil }
        let candidates = info.hostCandidates(limit: 60)
        let semaphore = DispatchSemaphore(value: 0)
        let foundLock = NSLock()
        var found: URL?
        var pending = candidates.count
        let limiter = DispatchSemaphore(value: 12)

        for host in candidates {
            limiter.wait()
            DispatchQueue.global(qos: .utility).async {
                defer { limiter.signal() }
                if foundLock.try() {
                    let already = found != nil
                    foundLock.unlock()
                    if already {
                        finish()
                        return
                    }
                }

                let base = URL(string: "http://\(host):8080")!
                if isHealthy(baseURL: base, timeout: timeoutSeconds) {
                    foundLock.lock()
                    if found == nil {
                        found = base
                    }
                    foundLock.unlock()
                }
                finish()
            }
        }

        func finish() {
            foundLock.lock()
            pending -= 1
            let done = pending == 0 || found != nil
            foundLock.unlock()
            if done {
                semaphore.signal()
            }
        }

        _ = semaphore.wait(timeout: .now() + 3.0)
        return found
    }

    private static func isHealthy(baseURL: URL, timeout: TimeInterval) -> Bool {
        let url = baseURL.appendingPathComponent("/actuator/health")
        var request = URLRequest(url: url, timeoutInterval: timeout)
        request.httpMethod = "GET"
        let semaphore = DispatchSemaphore(value: 0)
        var ok = false
        URLSession.shared.dataTask(with: request) { data, response, _ in
            defer { semaphore.signal() }
            guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode) else {
                return
            }
            guard let data, !data.isEmpty else { return }
            if let obj = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
               let status = obj["status"] as? String,
               status.uppercased() == "UP" {
                ok = true
            }
        }.resume()
        _ = semaphore.wait(timeout: .now() + timeout + 0.2)
        return ok
    }
}

private struct LocalNetworkInfo {
    var ip: in_addr
    var netmask: in_addr

    static func wifiIpv4() -> LocalNetworkInfo? {
        var ptr: UnsafeMutablePointer<ifaddrs>?
        guard getifaddrs(&ptr) == 0, let first = ptr else { return nil }
        defer { freeifaddrs(first) }

        var p = first
        while true {
            let ifa = p.pointee
            if let addr = ifa.ifa_addr, addr.pointee.sa_family == UInt8(AF_INET) {
                let name = String(cString: ifa.ifa_name)
                if name == "en0" {
                    let sa = UnsafeRawPointer(addr).assumingMemoryBound(to: sockaddr_in.self).pointee
                    let nmPtr = ifa.ifa_netmask
                    if let nmPtr {
                        let nm = UnsafeRawPointer(nmPtr).assumingMemoryBound(to: sockaddr_in.self).pointee
                        return LocalNetworkInfo(ip: sa.sin_addr, netmask: nm.sin_addr)
                    }
                }
            }
            if let next = ifa.ifa_next {
                p = next
            } else {
                break
            }
        }
        return nil
    }

    func hostCandidates(limit: Int) -> [String] {
        let ipU = UInt32(bigEndian: ip.s_addr)
        let maskU = UInt32(bigEndian: netmask.s_addr)
        if maskU == 0 {
            return []
        }
        let network = ipU & maskU
        let broadcast = network | (~maskU)
        let base = network + 1
        let end = broadcast - 1
        if end <= base {
            return []
        }
        var out: [String] = []
        let selfIp = ipU
        func toStr(_ v: UInt32) -> String {
            let a = (v >> 24) & 0xff
            let b = (v >> 16) & 0xff
            let c = (v >> 8) & 0xff
            let d = v & 0xff
            return "\(a).\(b).\(c).\(d)"
        }
        var preferred: [UInt32] = []
        preferred.append(selfIp == base ? base + 1 : base)
        preferred.append(selfIp & 0xffffff00 | 1)
        preferred.append(selfIp & 0xffffff00 | 84)
        for v in preferred {
            if v >= base, v <= end, v != selfIp, !out.contains(toStr(v)) {
                out.append(toStr(v))
                if out.count >= limit { return out }
            }
        }
        var v = base
        while v <= end, out.count < limit {
            if v != selfIp {
                out.append(toStr(v))
            }
            v += 1
        }
        return out
    }
}
