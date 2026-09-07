/* Tabel bookmarks: lowongan yang disimpan oleh pengguna. */
exports.up = (pgm) => {
  pgm.createTable('bookmarks', {
    id: { type: 'VARCHAR(50)', primaryKey: true },
    user_id: { type: 'VARCHAR(50)', notNull: true },
    job_id: { type: 'VARCHAR(50)', notNull: true },
    created_at: { type: 'TIMESTAMP', notNull: true, default: pgm.func('current_timestamp') },
  });

  pgm.addConstraint('bookmarks', 'fk_bookmarks.user_id_users.id',
    'FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE');
  pgm.addConstraint('bookmarks', 'fk_bookmarks.job_id_jobs.id',
    'FOREIGN KEY(job_id) REFERENCES jobs(id) ON DELETE CASCADE');
};

exports.down = (pgm) => { pgm.dropTable('bookmarks'); };
