class NewReply {
  constructor(payload) {
    this._verifyPayload(payload);

    const { content, commentId, owner } = payload;

    this.content = content;
    this.commentId = commentId;
    this.owner = owner;
  }

  _verifyPayload({ content, commentId, owner }) {
    if (!content || !commentId || !owner) {
      throw new Error('NEW_REPLY.NOT_CONTAIN_NEEDED_PROPERTY');
    }

    if (typeof content !== 'string' || typeof commentId !== 'string' || typeof owner !== 'string') {
      throw new Error('NEW_REPLY.NOT_MEET_DATA_TYPE_SPECIFICATION');
    }
  }
}

export default NewReply;
