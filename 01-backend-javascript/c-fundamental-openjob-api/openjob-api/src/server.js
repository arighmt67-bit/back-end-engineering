/* Titik masuk aplikasi OpenJob RESTful API. */
require('dotenv').config();
const express = require('express');

const usersRouter = require('./routes/users');
const authenticationsRouter = require('./routes/authentications');
const companiesRouter = require('./routes/companies');
const categoriesRouter = require('./routes/categories');
const jobsRouter = require('./routes/jobs');
const applicationsRouter = require('./routes/applications');
const bookmarksRouter = require('./routes/bookmarks');
const profileRouter = require('./routes/profile');
const documentsRouter = require('./routes/documents');
const { errorHandler, notFoundHandler } = require('./middlewares/errorHandler');

const app = express();
app.use(express.json());

/* Pendaftaran seluruh route */
app.use('/users', usersRouter);
app.use('/authentications', authenticationsRouter);
app.use('/companies', companiesRouter);
app.use('/categories', categoriesRouter);
app.use('/jobs', jobsRouter);
app.use('/applications', applicationsRouter);
app.use('/bookmarks', bookmarksRouter);
app.use('/profile', profileRouter);
app.use('/documents', documentsRouter);

/* Penanganan route tidak dikenal dan error terpusat */
app.use(notFoundHandler);
app.use(errorHandler);

const port = process.env.PORT || 3000;
const host = process.env.HOST || 'localhost';

app.listen(port, host, () => {
  console.log(`Server berjalan pada http://${host}:${port}`);
});

module.exports = app;
