document.addEventListener("DOMContentLoaded", function () {
  const navItems = document.querySelectorAll(".nav-container .item");
  if (!navItems.length) return;

  let observer = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          document.querySelectorAll(".nav-container .item").forEach((link) => {
            link.classList.remove("active");
          });
          document
            .querySelector(
              '.nav-container .item[href="#' + entry.target.id + '"]'
            )
            .classList.add("active");
        }
      });
    },
    {
      rootMargin: "0px 0px -70% 0px",
    }
  );

  navItems.forEach((link) => {
    observer.observe(document.querySelector(link.getAttribute("href")));
  });
});
