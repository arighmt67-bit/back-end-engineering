#!/bin/bash
apt-get update -y
apt-get install -y nginx

cat << 'EOF' > /var/www/html/index.html
<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Profil Ari Rahmat Romadhon</title>
    <link rel="stylesheet" href="style.css">
</head>
<body>
    <div class="container">
        <header class="card header-card">
            <div class="avatar-container">
                <img src="https://storage.googleapis.com/web-profil-arirahmatr/profile.jpg" alt="Foto Profil Ari Rahmat Romadhon" class="avatar">
            </div>
            <div class="header-info">
                <h1>Ari Rahmat Romadhon</h1>
                <p class="title">IT Support & Cloud / DevOps Enthusiast</p>
                <div class="badge-container">
                    <span class="badge">Google Cloud</span>
                    <span class="badge">AWS re/Start Graduate</span>
                    <span class="badge">Linux & Networking</span>
                </div>
            </div>
        </header>

        <main class="grid">
            <section class="card section-card">
                <h2>Tentang Saya</h2>
                <p>
                    Profesional IT dengan latar belakang pendidikan S1 dan pengalaman di bidang IT Support, infrastruktur sistem, dan jaringan. Saat ini sedang memperdalam kompetensi dalam ekosistem Cloud Computing (Google Cloud Platform & AWS), Containerization (Docker), serta otomasi CI/CD dan Linux Administration.
                </p>
            </section>

            <section class="card section-card">
                <h2>Keahlian & Teknologi</h2>
                <ul class="skills-list">
                    <li><strong>Cloud Platforms:</strong> Google Cloud Platform (Compute Engine, Cloud Storage, VPC), AWS</li>
                    <li><strong>Operating Systems:</strong> Linux (Ubuntu, Debian), macOS, Windows Server</li>
                    <li><strong>DevOps & Tools:</strong> Docker, Git, GitHub, Bash Scripting, NGINX</li>
                    <li><strong>Networking:</strong> TCP/IP, DNS, Subnetting, Firewall Configuration</li>
                </ul>
            </section>

            <section class="card section-card">
                <h2>Kontak & Informasi</h2>
                <div class="contact-info">
                    <p><strong>Email:</strong> arirahmatromadhon@gmail.com</p>
                    <p><strong>GitHub:</strong> <a href="https://github.com/arighmt67-bit" target="_blank" rel="noopener noreferrer">github.com/arighmt67-bit</a></p>
                    <p><strong>Lokasi:</strong> Indonesia</p>
                </div>
            </section>
        </main>

        <footer>
            <p>&copy; 2026 Ari Rahmat Romadhon. Hosted on Google Compute Engine & Google Cloud Storage.</p>
        </footer>
    </div>
</body>
</html>
EOF

cat << 'EOF' > /var/www/html/style.css
* {
    box-sizing: border-box;
    margin: 0;
    padding: 0;
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Oxygen, Ubuntu, Cantarell, "Fira Sans", "Droid Sans", "Helvetica Neue", sans-serif;
}

body {
    background-color: #f4f6f9;
    color: #333333;
    line-height: 1.6;
    padding: 40px 20px;
}

.container {
    max-width: 800px;
    margin: 0 auto;
}

.card {
    background: #ffffff;
    border-radius: 12px;
    padding: 24px;
    margin-bottom: 24px;
    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
    border: 1px solid #e5e7eb;
}

.header-card {
    display: flex;
    align-items: center;
    gap: 24px;
}

.avatar-container {
    flex-shrink: 0;
}

.avatar {
    width: 120px;
    height: 120px;
    border-radius: 50%;
    object-fit: cover;
    border: 4px solid #1a73e8;
    box-shadow: 0 2px 8px rgba(26, 115, 232, 0.25);
}

.header-info h1 {
    font-size: 24px;
    color: #1f2937;
    margin-bottom: 6px;
}

.header-info .title {
    font-size: 16px;
    color: #4b5563;
    margin-bottom: 12px;
    font-weight: 500;
}

.badge-container {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
}

.badge {
    background-color: #e8f0fe;
    color: #1a73e8;
    padding: 4px 12px;
    border-radius: 16px;
    font-size: 12px;
    font-weight: 600;
    border: 1px solid #cce0ff;
}

.section-card h2 {
    font-size: 18px;
    color: #111827;
    margin-bottom: 14px;
    border-bottom: 2px solid #1a73e8;
    padding-bottom: 6px;
    display: inline-block;
}

.skills-list {
    list-style: none;
    padding: 0;
}

.skills-list li {
    padding: 8px 0;
    border-bottom: 1px dashed #e5e7eb;
}

.skills-list li:last-child {
    border-bottom: none;
}

.contact-info p {
    margin-bottom: 8px;
}

.contact-info a {
    color: #1a73e8;
    text-decoration: none;
    font-weight: 500;
}

.contact-info a:hover {
    text-decoration: underline;
}

footer {
    text-align: center;
    color: #6b7280;
    font-size: 13px;
    margin-top: 20px;
}

@media (max-width: 600px) {
    .header-card {
        flex-direction: column;
        text-align: center;
    }
    
    .badge-container {
        justify-content: center;
    }
}
EOF

chown -R www-data:www-data /var/www/html
chmod -R 755 /var/www/html
systemctl restart nginx
systemctl enable nginx
