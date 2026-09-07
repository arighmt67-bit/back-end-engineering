/* Tabel authentications: menyimpan refresh token yang masih valid. */
exports.up = (pgm) => {
  pgm.createTable('authentications', {
    token: { type: 'TEXT', notNull: true },
  });
};

exports.down = (pgm) => { pgm.dropTable('authentications'); };
