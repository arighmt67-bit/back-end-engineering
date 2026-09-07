import RepliesHandler from './handler.js';
import createRepliesRouter from './routes.js';

export default (container) => {
  const repliesHandler = new RepliesHandler(container);
  return createRepliesRouter(repliesHandler);
};
