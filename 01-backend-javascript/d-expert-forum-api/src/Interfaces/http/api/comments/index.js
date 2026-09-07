import CommentsHandler from './handler.js';
import createCommentsRouter from './routes.js';

export default (container) => {
  const commentsHandler = new CommentsHandler(container);
  return createCommentsRouter(commentsHandler);
};
