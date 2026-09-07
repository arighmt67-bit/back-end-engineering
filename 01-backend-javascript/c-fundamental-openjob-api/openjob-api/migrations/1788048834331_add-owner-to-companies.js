/* Menambah kolom owner_id pada companies agar pemilik lowongan dapat ditelusuri.
   Dibutuhkan consumer RabbitMQ untuk mengirim email ke pemilik pekerjaan. */
exports.up = (pgm) => {
  pgm.addColumn('companies', {
    owner_id: { type: 'VARCHAR(50)' },
  });

  pgm.addConstraint('companies', 'fk_companies.owner_id_users.id',
    'FOREIGN KEY(owner_id) REFERENCES users(id) ON DELETE SET NULL');
};

exports.down = (pgm) => {
  pgm.dropConstraint('companies', 'fk_companies.owner_id_users.id');
  pgm.dropColumn('companies', 'owner_id');
};
