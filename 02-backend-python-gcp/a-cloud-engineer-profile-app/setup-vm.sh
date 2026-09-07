#!/bin/bash
set -e

echo "=== [1/4] Updating package lists & installing Nginx ==="
sudo apt-get update -y
sudo apt-get install -y nginx

echo "=== [2/4] Configuring Web Root Directory ==="
sudo rm -rf /var/www/html/*

echo "=== [3/4] Copying Profile Application Files ==="
sudo tee /var/www/html/index.html > /dev/null <<'EOF'
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Ari Rahmat | Cloud & DevOps Enthusiast</title>
  <link rel="stylesheet" href="style.css">
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@300;400;500;600;700;800&display=swap" rel="stylesheet">
</head>
<body>
  <div class="background-glow"></div>
  
  <div class="container">
    <!-- Header Banner & Avatar Section -->
    <header class="profile-header">
      <div class="banner-wrapper">
        <img src="https://storage.googleapis.com/arirahmat-profile-bucket/banner.jpg" alt="Profile Banner" class="profile-banner">
      </div>
      <div class="profile-intro">
        <div class="avatar-wrapper">
          <img src="https://storage.googleapis.com/arirahmat-profile-bucket/profile.jpg" alt="Ari Rahmat" class="profile-avatar">
        </div>
        <div class="profile-title">
          <h1>Ari Rahmat</h1>
          <div class="badge-tag">Tech Support &rarr; Cloud & DevOps Aspirant</div>
          <p class="tagline">Transitioning from hands-on IT & Technical Support into scalable Cloud Infrastructure, Automation, and DevOps Engineering.</p>
        </div>
      </div>
    </header>

    <!-- Main Grid Content -->
    <main class="main-content">
      <!-- About Section -->
      <section class="card about-card">
        <div class="card-header">
          <span class="card-icon">👤</span>
          <h2>About Me</h2>
        </div>
        <p>
          Experienced in technical support, system troubleshooting, and infrastructure administration. Currently leveling up through intensive cloud learning paths (AWS re/Start Graduate, Google Cloud Architecture, and DevOps practices). Passionate about Linux systems, containerization, and automated CI/CD workflows.
        </p>
      </section>

      <!-- Technical Focus & Skills -->
      <section class="card skills-card">
        <div class="card-header">
          <span class="card-icon">⚡</span>
          <h2>Technical Focus & Toolchain</h2>
        </div>
        <div class="skills-grid">
          <div class="skill-category">
            <h3>Cloud & Platform</h3>
            <div class="tags">
              <span class="tag">Google Cloud (GCP)</span>
              <span class="tag">Compute Engine</span>
              <span class="tag">Cloud Storage</span>
              <span class="tag">AWS Core Services</span>
            </div>
          </div>
          <div class="skill-category">
            <h3>DevOps & Automation</h3>
            <div class="tags">
              <span class="tag">Docker</span>
              <span class="tag">Linux / Bash Scripting</span>
              <span class="tag">CI/CD Fundamentals</span>
              <span class="tag">Git & GitHub</span>
            </div>
          </div>
          <div class="skill-category">
            <h3>Support & Infrastructure</h3>
            <div class="tags">
              <span class="tag">Network Diagnostics</span>
              <span class="tag">System Troubleshooting</span>
              <span class="tag">IT Support Operations</span>
              <span class="tag">Security Baseline</span>
            </div>
          </div>
        </div>
      </section>

      <!-- Cloud Journey & Milestones -->
      <section class="card journey-card">
        <div class="card-header">
          <span class="card-icon">🚀</span>
          <h2>Learning Milestones & Certifications</h2>
        </div>
        <ul class="timeline">
          <li class="timeline-item">
            <div class="timeline-dot"></div>
            <div class="timeline-content">
              <h4>Google Cloud Engineer Learning Path</h4>
              <p class="timeline-sub">Dicoding Academy — Compute Engine, Cloud Storage & IAM Deployment</p>
            </div>
          </li>
          <li class="timeline-item">
            <div class="timeline-dot"></div>
            <div class="timeline-content">
              <h4>AWS re/Start Graduate</h4>
              <p class="timeline-sub">Comprehensive Cloud Infrastructure, Linux administration & Networking</p>
            </div>
          </li>
          <li class="timeline-item">
            <div class="timeline-dot"></div>
            <div class="timeline-content">
              <h4>IT Technical Support Foundations</h4>
              <p class="timeline-sub">Hardware, OS maintenance, networking diagnostics, and operational support</p>
            </div>
          </li>
        </ul>
      </section>

      <!-- Connect Section -->
      <section class="card connect-card">
        <div class="card-header">
          <span class="card-icon">📫</span>
          <h2>Get In Touch</h2>
        </div>
        <div class="contact-links">
          <a href="mailto:arirahmatromadhon@gmail.com" class="contact-button email">
            <span>✉️</span> arirahmatromadhon@gmail.com
          </a>
          <a href="https://github.com/arighmt67-bit" target="_blank" rel="noopener noreferrer" class="contact-button github">
            <span>🐙</span> GitHub: arighmt67-bit
          </a>
        </div>
      </section>
    </main>

    <!-- Footer -->
    <footer class="profile-footer">
      <p>Hosted on <strong>Google Compute Engine (GCE)</strong> &bull; Static Assets on <strong>Google Cloud Storage (GCS)</strong></p>
      <p class="copyright">&copy; 2026 Ari Rahmat. Built for Dicoding GCP Submission.</p>
    </footer>
  </div>
</body>
</html>

EOF

sudo tee /var/www/html/style.css > /dev/null <<'EOF'
:root {
  --bg-color: #0b0f19;
  --card-bg: rgba(18, 24, 38, 0.75);
  --card-border: rgba(255, 255, 255, 0.08);
  --card-hover-border: rgba(99, 102, 241, 0.4);
  --primary-accent: #6366f1;
  --secondary-accent: #38bdf8;
  --text-main: #f8fafc;
  --text-muted: #94a3b8;
  --text-dim: #64748b;
  --tag-bg: rgba(99, 102, 241, 0.12);
  --tag-text: #a5b4fc;
  --tag-border: rgba(99, 102, 241, 0.25);
  --radius-lg: 16px;
  --radius-md: 10px;
  --radius-sm: 6px;
  --shadow: 0 10px 30px -10px rgba(0, 0, 0, 0.5);
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  background-color: var(--bg-color);
  color: var(--text-main);
  line-height: 1.6;
  min-height: 100vh;
  position: relative;
  overflow-x: hidden;
  padding: 2rem 1rem 3rem;
}

.background-glow {
  position: fixed;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 1000px;
  height: 600px;
  background: radial-gradient(circle at 50% 0%, rgba(99, 102, 241, 0.15), rgba(56, 189, 248, 0.08), transparent 70%);
  pointer-events: none;
  z-index: 0;
}

.container {
  max-width: 860px;
  margin: 0 auto;
  position: relative;
  z-index: 1;
}

/* Header & Banner */
.profile-header {
  background: var(--card-bg);
  border: 1px solid var(--card-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow);
  backdrop-filter: blur(16px);
  margin-bottom: 2rem;
}

.banner-wrapper {
  width: 100%;
  height: 180px;
  background: linear-gradient(135deg, #1e1b4b, #0f172a, #0369a1);
  position: relative;
  overflow: hidden;
}

.profile-banner {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.profile-intro {
  padding: 0 2rem 2rem;
  display: flex;
  flex-direction: column;
  position: relative;
  margin-top: -60px;
}

.avatar-wrapper {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  border: 4px solid var(--bg-color);
  background: #1e293b;
  overflow: hidden;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4);
  margin-bottom: 1rem;
}

.profile-avatar {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.profile-title h1 {
  font-size: 2rem;
  font-weight: 800;
  letter-spacing: -0.02em;
  color: #ffffff;
  margin-bottom: 0.35rem;
}

.badge-tag {
  display: inline-block;
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--secondary-accent);
  background: rgba(56, 189, 248, 0.1);
  border: 1px solid rgba(56, 189, 248, 0.25);
  padding: 0.25rem 0.85rem;
  border-radius: 20px;
  margin-bottom: 0.75rem;
}

.tagline {
  color: var(--text-muted);
  font-size: 1rem;
  max-width: 700px;
}

/* Main Content Cards */
.main-content {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.card {
  background: var(--card-bg);
  border: 1px solid var(--card-border);
  border-radius: var(--radius-lg);
  padding: 1.75rem;
  box-shadow: var(--shadow);
  backdrop-filter: blur(12px);
  transition: transform 0.2s ease, border-color 0.2s ease;
}

.card:hover {
  border-color: var(--card-hover-border);
  transform: translateY(-2px);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 1.25rem;
}

.card-icon {
  font-size: 1.35rem;
}

.card-header h2 {
  font-size: 1.25rem;
  font-weight: 700;
  color: #ffffff;
  letter-spacing: -0.01em;
}

.about-card p {
  color: var(--text-muted);
  font-size: 0.98rem;
  line-height: 1.7;
}

/* Skills Grid */
.skills-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 1.25rem;
}

.skill-category {
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(255, 255, 255, 0.04);
  padding: 1.25rem;
  border-radius: var(--radius-md);
}

.skill-category h3 {
  font-size: 0.9rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--secondary-accent);
  margin-bottom: 0.85rem;
  font-weight: 700;
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.tag {
  background: var(--tag-bg);
  color: var(--tag-text);
  border: 1px solid var(--tag-border);
  font-size: 0.8rem;
  font-weight: 500;
  padding: 0.25rem 0.65rem;
  border-radius: var(--radius-sm);
}

/* Timeline */
.timeline {
  list-style: none;
  position: relative;
  padding-left: 1.5rem;
}

.timeline::before {
  content: '';
  position: absolute;
  left: 6px;
  top: 6px;
  bottom: 6px;
  width: 2px;
  background: rgba(255, 255, 255, 0.1);
}

.timeline-item {
  position: relative;
  margin-bottom: 1.25rem;
}

.timeline-item:last-child {
  margin-bottom: 0;
}

.timeline-dot {
  position: absolute;
  left: -1.5rem;
  top: 6px;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: var(--primary-accent);
  border: 3px solid var(--bg-color);
  box-shadow: 0 0 10px var(--primary-accent);
}

.timeline-content h4 {
  font-size: 0.98rem;
  font-weight: 600;
  color: #ffffff;
}

.timeline-sub {
  font-size: 0.85rem;
  color: var(--text-muted);
  margin-top: 0.2rem;
}

/* Contact Links */
.contact-links {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
}

.contact-button {
  display: inline-flex;
  align-items: center;
  gap: 0.6rem;
  padding: 0.75rem 1.25rem;
  border-radius: var(--radius-md);
  text-decoration: none;
  font-size: 0.9rem;
  font-weight: 600;
  transition: all 0.2s ease;
}

.contact-button.email {
  background: rgba(99, 102, 241, 0.15);
  color: #c7d2fe;
  border: 1px solid rgba(99, 102, 241, 0.3);
}

.contact-button.email:hover {
  background: rgba(99, 102, 241, 0.25);
  border-color: rgba(99, 102, 241, 0.5);
  transform: translateY(-2px);
}

.contact-button.github {
  background: rgba(255, 255, 255, 0.05);
  color: #f1f5f9;
  border: 1px solid rgba(255, 255, 255, 0.15);
}

.contact-button.github:hover {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.3);
  transform: translateY(-2px);
}

/* Footer */
.profile-footer {
  text-align: center;
  margin-top: 3rem;
  padding-top: 1.5rem;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
  color: var(--text-dim);
  font-size: 0.85rem;
}

.profile-footer strong {
  color: var(--text-muted);
}

.copyright {
  margin-top: 0.35rem;
  font-size: 0.8rem;
}

/* Mobile Responsiveness */
@media (max-width: 640px) {
  body {
    padding: 1rem 0.75rem 2rem;
  }
  
  .banner-wrapper {
    height: 140px;
  }
  
  .profile-intro {
    padding: 0 1.25rem 1.5rem;
    margin-top: -50px;
  }
  
  .avatar-wrapper {
    width: 100px;
    height: 100px;
  }
  
  .profile-title h1 {
    font-size: 1.6rem;
  }
  
  .card {
    padding: 1.25rem;
  }
  
  .contact-links {
    flex-direction: column;
  }
  
  .contact-button {
    width: 100%;
    justify-content: center;
  }
}

EOF

echo "=== [4/4] Setting Permissions & Restarting Nginx ==="
sudo chown -R www-data:www-data /var/www/html
sudo chmod -R 755 /var/www/html
sudo systemctl restart nginx
sudo systemctl enable nginx

echo "================================================="
echo "✅ DEPLOYMENT SUKSES! Web Profile Nginx sudah aktif."
echo "Silakan buka http://$(curl -s ifconfig.me) di browser."
echo "================================================="
