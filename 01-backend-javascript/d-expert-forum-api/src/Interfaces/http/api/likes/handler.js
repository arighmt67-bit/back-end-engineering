import LikeUnlikeCommentUseCase from '../../../../Applications/use_case/LikeUnlikeCommentUseCase.js';

class LikesHandler {
  constructor(container) {
    this._container = container;

    this.putLikeHandler = this.putLikeHandler.bind(this);
  }

  async putLikeHandler(req, res, next) {
    try {
      const { id: owner } = req.auth.credentials;
      const { threadId, commentId } = req.params;

      const likeUnlikeCommentUseCase = this._container
        .getInstance(LikeUnlikeCommentUseCase.name);
      await likeUnlikeCommentUseCase.execute({ threadId, commentId, owner });

      res.json({ status: 'success' });
    } catch (error) {
      next(error);
    }
  }
}

export default LikesHandler;
