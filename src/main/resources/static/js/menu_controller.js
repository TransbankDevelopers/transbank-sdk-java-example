document.addEventListener("DOMContentLoaded", function () {
  const theme = localStorage.getItem("theme");
  const root = document.documentElement;
  if (theme === "dark") {
    root.setAttribute("data-theme", "dark");
    root.classList.remove("light");
    root.classList.add("dark");
    localStorage.setItem("theme", "dark");
  } else {
    root.setAttribute("data-theme", "light");
    root.classList.add("light");
    root.classList.remove("dark");
  }

  function setTheme() {
    const currentTheme = localStorage.getItem("theme");
    if (currentTheme === "dark") {
      root.setAttribute("data-theme", "light");
      root.classList.remove("dark");
      root.classList.add("light");
      localStorage.setItem("theme", "light");
    } else {
      root.setAttribute("data-theme", "dark");
      root.classList.remove("light");
      root.classList.add("dark");
      localStorage.setItem("theme", "dark");
    }
  }

  const darkModeButton = document.getElementById("dark-mode");
  if (darkModeButton) {
    darkModeButton.addEventListener("click", function () {
      setTheme();
    });
  }
});
