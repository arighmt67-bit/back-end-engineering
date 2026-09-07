/* Tabel applications: lamaran pengguna terhadap sebuah lowongan. */
exports.up = (pgm) => {
  pgm.createTable('applications', {
    id: { type: 'VARCHAR(50)', primaryKey: true },
    user_id: { type: 'VARCHAR(50)', notNull: true },
    job_id: { type: 'VARCHAR(50)', notNull: true },
    status: { type: 'VARCHAR(20)', notNull: true, default: 'pending' },
    created_at: { type: 'TIMESTAMP', notNull: true, default: pgm.func('current_timestamp') },
    updated_at: { type: 'TIMESTAMP', notNull: true, default: pgm.func('current_timestamp') },
  });

  pgm.addConstraint('applications', 'fk_applications.user_id_users.id',
    'FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE');
  pgm.addConstraint('applications', 'fk_applications.job_id_jobs.id',
    'FOREIGN KEY(job_id) REFERENCES jobs(id) ON DELETE CASCADE');
};

exports.down = (pgm) => { pgm.dropTable('applications'); };
