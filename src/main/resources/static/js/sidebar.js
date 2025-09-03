document.addEventListener("DOMContentLoaded", function () {
  const toggleBtn = document.getElementById("toggle-sidebar-btn");
  const sidebar = document.getElementById("sidebar-desk");
  const bodyContainer = document.querySelector(".body-container");

  let isMenuVisible = true;
  toggleBtn.addEventListener("click", () => {
    isMenuVisible = !isMenuVisible;
    if (!isMenuVisible) {
      sidebar.classList.add("tbk-sidebar-hide");
      bodyContainer.classList.add("hide-sd");
    } else {
      sidebar.classList.remove("tbk-sidebar-hide");
      bodyContainer.classList.remove("hide-sd");
    }
  });

  const collapsibleTitles = document.querySelectorAll(
    ".sidebar-collapsible-title"
  );
  collapsibleTitles.forEach((title) => {
    title.addEventListener("click", () => {
      const content = title.nextElementSibling;
      const arrow = title.querySelector(".arrow");
      if (!content) return;

      content.classList.toggle("open");
      arrow.classList.toggle("sidebar-icons-rotate");
    });
  });

  function highlightByCurrentPath(currentPath) {
    document.querySelectorAll("li.collapsible-items > a").forEach((anchor) => {
      const linkPath = anchor.getAttribute("href") || "";

      if (currentPath === linkPath) {
        const liItem = anchor.parentElement;
        liItem.classList.add("active");

        const collapsibleUl = liItem.closest(".collapsible-content");
        if (collapsibleUl) {
          collapsibleUl.classList.add("open");

          const collapsibleButton = collapsibleUl.previousElementSibling;
          if (collapsibleButton) {
            const icon = collapsibleButton.querySelector("img");
            if (icon) icon.classList.add("sidebar-icons-rotate");
          }
        }
      }
    });
  }

  function highlightPrincipalPath(principalPath) {
    document.querySelectorAll("li.collapsible-items > a").forEach((anchor) => {
      const linkPath = anchor.getAttribute("href") || "";
      const principalLinkPath = linkPath.split("/")[1];
      const activeCurrentAnchor = principalLinkPath === principalPath;

      if (activeCurrentAnchor) {
        const liItem = anchor.parentElement;
        liItem.classList.add("active");

        const collapsibleUl = liItem.closest(".collapsible-content");
        if (collapsibleUl) {
          collapsibleUl.classList.add("open");

          const collapsibleButton = collapsibleUl.previousElementSibling;
          if (collapsibleButton) {
            const icon = collapsibleButton.querySelector("img");
            if (icon) icon.classList.add("sidebar-icons-rotate");
          }
        }
      }
    });
  }

  (function highlightActiveByUrl() {
    const currentPath = window.location.pathname;
    const principalPath = currentPath.split("/")[1];

    if (currentPath.startsWith("/api-reference")) {
      highlightByCurrentPath(currentPath);
    } else {
      highlightPrincipalPath(principalPath);
    }
  })();

  // mobile sidebar
  const burgerMenu = document.getElementById("burger-menu");
  const mobileSdContainer = document.querySelector(".tbk-sidebar-mobile");
  const closeBtn = document.getElementById("close-sidebar-btn");

  burgerMenu.addEventListener("click", () => {
    mobileSdContainer.classList.toggle("show");
  });
  closeBtn.addEventListener("click", () => {
    mobileSdContainer.classList.remove("show");
  });
});
