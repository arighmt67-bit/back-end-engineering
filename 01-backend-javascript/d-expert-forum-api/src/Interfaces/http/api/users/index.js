import UsersHandler from './handler.js';
import createUsersRouter from './routes.js';

export default (container) => {
  const usersHandler = new UsersHandler(container);
  return createUsersRouter(usersHandler);
};
