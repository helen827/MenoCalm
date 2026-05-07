import { Fragment, useCallback, useEffect, useState } from "react";
import { fetchAdminPostsPage, fetchPostComments, getStoredUserId, hideAdminComment, hideAdminPost } from "./api";
import type { CommunityComment, CommunityPost } from "./types";

type Props = {
  onLogout: () => void;
};

export function PostsModeration({ onLogout }: Props) {
  const [items, setItems] = useState<CommunityPost[]>([]);
  const [cursor, setCursor] = useState<string | null>(null);
  const [hasMore, setHasMore] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [expanded, setExpanded] = useState<Record<string, boolean>>({});
  const [commentsByPost, setCommentsByPost] = useState<Record<string, CommunityComment[]>>({});
  const [commentsLoading, setCommentsLoading] = useState<Record<string, boolean>>({});
  const [commentsError, setCommentsError] = useState<Record<string, string>>({});

  const userId = getStoredUserId();

  const loadPage = useCallback(async (nextCursor?: string | null, append = false) => {
    setError(null);
    setLoading(true);
    try {
      const page = await fetchAdminPostsPage(nextCursor ?? undefined, 30);
      const chunk = page.items ?? [];
      setItems((prev) => (append ? [...prev, ...chunk] : chunk));
      setCursor(page.nextCursor ?? null);
      setHasMore(Boolean(page.hasMore));
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadPage(null, false);
  }, [loadPage]);

  async function refresh() {
    setExpanded({});
    setCommentsByPost({});
    await loadPage(null, false);
  }

  async function loadComments(postId: string) {
    setCommentsLoading((m) => ({ ...m, [postId]: true }));
    setCommentsError((m) => {
      const { [postId]: _, ...rest } = m;
      return rest;
    });
    try {
      const list = await fetchPostComments(postId);
      setCommentsByPost((m) => ({ ...m, [postId]: list }));
    } catch (e) {
      const msg = e instanceof Error ? e.message : String(e);
      setCommentsError((m) => ({ ...m, [postId]: msg }));
    } finally {
      setCommentsLoading((m) => ({ ...m, [postId]: false }));
    }
  }

  function toggleExpand(postId: string) {
    setExpanded((m) => {
      const next = !m[postId];
      if (next && !commentsByPost[postId]) {
        void loadComments(postId);
      }
      return { ...m, [postId]: next };
    });
  }

  async function onHidePost(postId: string) {
    if (!window.confirm(`确定下架帖子 ${postId}？`)) {
      return;
    }
    try {
      await hideAdminPost(postId);
      await refresh();
    } catch (e) {
      alert(e instanceof Error ? e.message : String(e));
    }
  }

  async function onHideComment(postId: string, commentId: string) {
    if (!window.confirm(`确定删除评论 ${commentId}？`)) {
      return;
    }
    try {
      await hideAdminComment(postId, commentId);
      await loadComments(postId);
    } catch (e) {
      alert(e instanceof Error ? e.message : String(e));
    }
  }

  return (
    <>
      <div className="card" style={{ display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: 12 }}>
        <div>
          <strong>当前用户</strong>：<code>{userId || "（未知，评论接口可能失败）"}</code>
        </div>
        <div className="row-actions">
          <button type="button" className="btn-ghost" onClick={() => void refresh()} disabled={loading}>
            刷新列表
          </button>
          <button
            type="button"
            className="btn-ghost"
            onClick={() => onLogout()}
          >
            退出登录
          </button>
        </div>
      </div>

      {error ? (
        <div className="card err" style={{ whiteSpace: "pre-wrap" }}>
          {error}
        </div>
      ) : null}

      <div className="card">
        <h2 style={{ marginTop: 0, fontSize: 17 }}>全站帖子</h2>
        {loading && items.length === 0 ? <p>加载中…</p> : null}
        <div style={{ overflowX: "auto" }}>
          <table>
            <thead>
              <tr>
                <th>状态</th>
                <th>ID</th>
                <th>作者</th>
                <th>标题</th>
                <th>时间</th>
                <th>互动</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              {items.map((p) => (
                <Fragment key={p.id}>
                  <tr key={`${p.id}-row`}>
                    <td>
                      {p.deleted ? <span className="badge badge-deleted">已下架</span> : <span className="badge badge-live">展示中</span>}
                    </td>
                    <td>
                      <code style={{ fontSize: 11 }}>{p.id}</code>
                    </td>
                    <td>
                      <code style={{ fontSize: 11 }}>{p.authorUserId ?? "—"}</code>
                    </td>
                    <td style={{ maxWidth: 220 }}>{p.title ?? "（无标题）"}</td>
                    <td>{formatMs(p.createdAtMs)}</td>
                    <td>
                      赞 {p.likeCount ?? 0} / 评 {p.commentCount ?? 0}
                    </td>
                    <td>
                      <div className="row-actions">
                        <button type="button" className="btn-ghost" style={{ padding: "6px 10px", fontSize: 12 }} onClick={() => toggleExpand(p.id)}>
                          {expanded[p.id] ? "收起评论" : "评论"}
                        </button>
                        {!p.deleted ? (
                          <button
                            type="button"
                            className="btn-danger"
                            style={{ padding: "6px 10px", fontSize: 12 }}
                            onClick={() => void onHidePost(p.id)}
                          >
                            下架
                          </button>
                        ) : null}
                      </div>
                    </td>
                  </tr>
                  {expanded[p.id] ? (
                    <tr key={`${p.id}-comments`}>
                      <td colSpan={7} style={{ background: "#faf9ff" }}>
                        <CommentsBlock
                          postId={p.id}
                          loading={Boolean(commentsLoading[p.id])}
                          error={commentsError[p.id]}
                          comments={commentsByPost[p.id] ?? []}
                          onReload={() => void loadComments(p.id)}
                          onHideComment={(cid) => void onHideComment(p.id, cid)}
                        />
                      </td>
                    </tr>
                  ) : null}
                </Fragment>
              ))}
            </tbody>
          </table>
        </div>
        {hasMore ? (
          <div style={{ marginTop: 16 }}>
            <button type="button" className="btn-primary" disabled={loading || !cursor} onClick={() => void loadPage(cursor, true)}>
              {loading ? "加载中…" : "加载更多"}
            </button>
          </div>
        ) : null}
      </div>
    </>
  );
}

function formatMs(ms?: number): string {
  if (ms == null) {
    return "—";
  }
  const d = new Date(ms);
  return d.toLocaleString("zh-CN");
}

function CommentsBlock({
  postId,
  loading,
  error,
  comments,
  onReload,
  onHideComment,
}: {
  postId: string;
  loading: boolean;
  error?: string;
  comments: CommunityComment[];
  onReload: () => void;
  onHideComment: (commentId: string) => void;
}) {
  return (
    <div style={{ padding: "8px 4px" }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 8 }}>
        <strong style={{ fontSize: 13 }}>帖子 {postId} 的评论</strong>
        <button type="button" className="btn-ghost" style={{ padding: "4px 10px", fontSize: 12 }} onClick={onReload}>
          刷新评论
        </button>
      </div>
      {loading ? <p style={{ fontSize: 13 }}>加载评论…</p> : null}
      {error ? <p className="err">{error}</p> : null}
      {!loading && comments.length === 0 && !error ? <p style={{ fontSize: 13, color: "#666" }}>暂无评论</p> : null}
      <ul style={{ listStyle: "none", padding: 0, margin: 0 }}>
        {comments.map((c) => (
          <li
            key={c.id}
            style={{
              borderBottom: "1px solid #eceaf5",
              padding: "8px 0",
              display: "flex",
              justifyContent: "space-between",
              gap: 12,
              alignItems: "flex-start",
            }}
          >
            <div>
              <div style={{ fontSize: 11, color: "#666" }}>
                <code>{c.authorUserId ?? "—"}</code> · {formatMs(c.createdAtMs)}
              </div>
              <div style={{ fontSize: 13, marginTop: 4 }}>{c.content ?? ""}</div>
            </div>
            <button type="button" className="btn-danger" style={{ padding: "4px 8px", fontSize: 11, flexShrink: 0 }} onClick={() => onHideComment(c.id)}>
              删除
            </button>
          </li>
        ))}
      </ul>
    </div>
  );
}
