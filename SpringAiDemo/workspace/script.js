// ============================================
  Rampavesh Kumar - Portfolio Interactivity
  Mobile Nav, Form Handling, and Animations
  ============================================

document.addEventListener('DOMContentLoaded', () => {

    // --- Mobile Navigation Toggle ---
    const menuToggle = document.getElementById('menuToggle');
    const navList = document.querySelector('.nav-list');

    if (menuToggle && navList) {
        menuToggle.addEventListener('click', () => {
            const isOpen = navList.classList.toggle('open');
            // Close menu when clicking outside
            setTimeout(() => {
                if (isOpen) navList.classList.remove('open');
            }, 300);
        });
    }

    // --- Smooth Scroll for Anchor Links ---
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', function(e) {
            e.preventDefault();
            const targetId = this.getAttribute('href');
            if (targetId === '') return;

            const target = document.querySelector(targetId);
            if (target) {
                target.scrollIntoView({ behavior: 'smooth', block: 'start' });
            }
        });
    });

    // --- Contact Form Submission ---
    const contactForm = document.querySelector('.contact-form');

    if (contactForm) {
        contactForm.addEventListener('submit', (e) => {
            e.preventDefault();

            const name = document.getElementById('name').value.trim();
            const email = document.getElementById('email').value.trim();
            const message = document.getElementById('message').value.trim();

            if (!name || !email || !message) {
                alert('Please fill in all fields.');
                return;
            }

            // Simulate form submission
            const submitBtn = contactForm.querySelector('.btn-submit');
            submitBtn.textContent = 'Sending...';
            submitBtn.disabled = true;

            setTimeout(() => {
                alert(`Thank you, ${name}! Your message has been sent.\n\n(In a real app, this would send data to a server.)`);
                contactForm.reset();
                submitBtn.textContent = 'Send Message';
                submitBtn.disabled = false;
            }, 800);
        });
    }

    // --- Active Section Highlight ---
    const sections = document.querySelectorAll('.section');
    const header = document.querySelector('.header');

    sections.forEach(section => {
        section.addEventListener('scroll', () => {
            // Only update when section is near the top of viewport
            if (section.offsetTop <= window.innerHeight + 50) {
                header.classList.add('active');
            } else {
                header.classList.remove('active');
            }
        });
    });
});
