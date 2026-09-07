/* Tabel documents: menyimpan metadata berkas PDF (CV/Resume) yang diunggah kandidat. */
exports.up = (pgm) => {
  pgm.createTable('documents', {
    id: { type: 'VARCHAR(50)', primaryKey: true },
    user_id: { type: 'VARCHAR(50)', notNull: true },
    filename: { type: 'TEXT', notNull: true },
    original_name: { type: 'TEXT', notNull: true },
    mime_type: { type: 'VARCHAR(100)', notNull: true },
    size: { type: 'BIGINT', notNull: true },
    created_at: { type: 'TIMESTAMP', notNull: true, default: pgm.func('current_timestamp') },
  });

  pgm.addConstraint('documents', 'fk_documents.user_id_users.id',
    'FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE');
};

exports.down = (pgm) => { pgm.dropTable('documents'); };
