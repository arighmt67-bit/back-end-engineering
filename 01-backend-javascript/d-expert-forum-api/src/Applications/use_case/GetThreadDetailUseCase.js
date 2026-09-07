class GetThreadDetailUseCase {
  constructor({
    threadRepository, commentRepository, replyRepository, likeRepository,
  }) {
    this._threadRepository = threadRepository;
    this._commentRepository = commentRepository;
    this._replyRepository = replyRepository;
    this._likeRepository = likeRepository;
  }

  async execute(useCasePayload) {
    this._verifyPayload(useCasePayload);
    const { threadId } = useCasePayload;

    const thread = await this._threadRepository.getThreadById(threadId);
    const rawComments = await this._commentRepository.getCommentsByThreadId(threadId);
    const commentIds = rawComments.map((comment) => comment.id);
    const rawReplies = await this._replyRepository.getRepliesByCommentIds(commentIds);
    const rawLikeCounts = await this._likeRepository.getLikeCountsByCommentIds(commentIds);

    const comments = rawComments.map((comment) => ({
      id: comment.id,
      username: comment.username,
      date: comment.date,
      replies: rawReplies
        .filter((reply) => reply.comment_id === comment.id)
        .map((reply) => ({
          id: reply.id,
          content: reply.is_delete ? '**balasan telah dihapus**' : reply.content,
          date: reply.date,
          username: reply.username,
        })),
      content: comment.is_delete ? '**komentar telah dihapus**' : comment.content,
      likeCount: rawLikeCounts
        .find((like) => like.comment_id === comment.id)?.like_count ?? 0,
    }));

    return { ...thread, comments };
  }

  _verifyPayload({ threadId }) {
    if (!threadId) {
      throw new Error('GET_THREAD_DETAIL_USE_CASE.NOT_CONTAIN_THREAD_ID');
    }

    if (typeof threadId !== 'string') {
      throw new Error('GET_THREAD_DETAIL_USE_CASE.PAYLOAD_NOT_MEET_DATA_TYPE_SPECIFICATION');
    }
  }
}

export default GetThreadDetailUseCase;
