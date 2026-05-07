package com.livemore.api.service;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.teaopenapi.models.Config;
import com.livemore.api.config.AppProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AliyunSmsSender implements SmsSender {

    private final AppProperties appProperties;
    private final Client client;

    public AliyunSmsSender(AppProperties appProperties) {
        this.appProperties = appProperties;
        this.client = createClient(appProperties);
    }

    @Override
    public void sendLoginCode(String phoneDigits, String code) {
        AppProperties.Sms sms = appProperties.getAuth().getSms();
        ensureConfigured(sms);
        SendSmsRequest request = new SendSmsRequest()
                .setPhoneNumbers(phoneDigits)
                .setSignName(sms.getSignName())
                .setTemplateCode(sms.getTemplateCode())
                .setTemplateParam("{\"code\":\"" + code + "\"}");
        try {
            SendSmsResponse response = client.sendSms(request);
            String respCode = response.getBody() == null ? null : response.getBody().getCode();
            if (!"OK".equalsIgnoreCase(respCode)) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "sms_send_failed");
            }
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "sms_send_failed");
        }
    }

    private Client createClient(AppProperties properties) {
        AppProperties.Sms sms = properties.getAuth().getSms();
        Config config = new Config()
                .setAccessKeyId(sms.getAccessKeyId())
                .setAccessKeySecret(sms.getAccessKeySecret());
        config.setEndpoint("dysmsapi.aliyuncs.com");
        try {
            return new Client(config);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "sms_not_configured");
        }
    }

    private void ensureConfigured(AppProperties.Sms sms) {
        if (isBlank(sms.getAccessKeyId())
                || isBlank(sms.getAccessKeySecret())
                || isBlank(sms.getSignName())
                || isBlank(sms.getTemplateCode())) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "sms_not_configured");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
