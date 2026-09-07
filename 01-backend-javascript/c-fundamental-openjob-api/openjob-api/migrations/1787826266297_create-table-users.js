/* Tabel users: menyimpan data pengguna beserta kredensialnya. */
exports.up = (pgm) => {
  pgm.createTable('users', {
    id: { type: 'VARCHAR(50)', primaryKey: true },
    name: { type: 'TEXT', notNull: true },
    email: { type: 'VARCHAR(255)', notNull: true, unique: true },
    password: { type: 'TEXT', notNull: true },
    role: { type: 'VARCHAR(20)', notNull: true, default: 'user' },
    created_at: { type: 'TIMESTAMP', notNull: true, default: pgm.func('current_timestamp') },
    updated_at: { type: 'TIMESTAMP', notNull: true, default: pgm.func('current_timestamp') },
  });
};

exports.down = (pgm) => { pgm.dropTable('users'); };
