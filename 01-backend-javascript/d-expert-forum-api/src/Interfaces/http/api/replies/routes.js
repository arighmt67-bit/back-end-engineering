import express from 'express';
import authenticationMiddleware from '../../middlewares/authenticationMiddleware.js';

const createRepliesRouter = (handler) => {
  const router = express.Router({ mergeParams: true });

  router.post('/', authenticationMiddleware, handler.postReplyHandler);
  router.delete('/:replyId', authenticationMiddleware, handler.deleteReplyHandler);

  return router;
};

export default createRepliesRouter;
