/* Satu kandidat hanya boleh melamar satu lowongan sekali.
   Mencegah publish ganda ke RabbitMQ akibat lamaran duplikat. */
exports.up = (pgm) => {
  pgm.addConstraint('applications', 'unique_applications.user_id_and_job_id',
    'UNIQUE(user_id, job_id)');
};

exports.down = (pgm) => {
  pgm.dropConstraint('applications', 'unique_applications.user_id_and_job_id');
};
