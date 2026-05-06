export type CommunityPost = {
  id: string;
  authorUserId?: string;
  title?: string;
  content?: string;
  createdAtMs?: number;
  commentCount?: number;
  likeCount?: number;
  deleted?: boolean;
};

export type CommunityPostPage = {
  items?: CommunityPost[];
  nextCursor?: string | null;
  hasMore?: boolean;
};

export type CommunityComment = {
  id: string;
  authorUserId?: string;
  content?: string;
  createdAtMs?: number;
};

export type AuthTokenResponse = {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  userID?: string;
  userId?: string;
};
