import LikesHandler from './handler.js';
import createLikesRouter from './routes.js';

export default (container) => {
  const likesHandler = new LikesHandler(container);
  return createLikesRouter(likesHandler);
};
