/* Tabel jobs: lowongan pekerjaan, terhubung ke companies dan categories. */
exports.up = (pgm) => {
  pgm.createTable('jobs', {
    id: { type: 'VARCHAR(50)', primaryKey: true },
    company_id: { type: 'VARCHAR(50)', notNull: true },
    category_id: { type: 'VARCHAR(50)' },
    title: { type: 'TEXT', notNull: true },
    description: { type: 'TEXT' },
    job_type: { type: 'VARCHAR(50)' },
    experience_level: { type: 'VARCHAR(50)' },
    location_type: { type: 'VARCHAR(50)' },
    location_city: { type: 'TEXT' },
    salary_min: { type: 'BIGINT' },
    salary_max: { type: 'BIGINT' },
    is_salary_visible: { type: 'BOOLEAN', notNull: true, default: true },
    status: { type: 'VARCHAR(20)', notNull: true, default: 'open' },
    created_at: { type: 'TIMESTAMP', notNull: true, default: pgm.func('current_timestamp') },
    updated_at: { type: 'TIMESTAMP', notNull: true, default: pgm.func('current_timestamp') },
  });

  /* Relasi ke companies & categories. */
  pgm.addConstraint('jobs', 'fk_jobs.company_id_companies.id',
    'FOREIGN KEY(company_id) REFERENCES companies(id) ON DELETE CASCADE');
  pgm.addConstraint('jobs', 'fk_jobs.category_id_categories.id',
    'FOREIGN KEY(category_id) REFERENCES categories(id) ON DELETE SET NULL');
};

exports.down = (pgm) => { pgm.dropTable('jobs'); };
