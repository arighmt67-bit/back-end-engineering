import ThreadsHandler from './handler.js';
import createThreadsRouter from './routes.js';

export default (container) => {
  const threadsHandler = new ThreadsHandler(container);
  return createThreadsRouter(threadsHandler);
};
