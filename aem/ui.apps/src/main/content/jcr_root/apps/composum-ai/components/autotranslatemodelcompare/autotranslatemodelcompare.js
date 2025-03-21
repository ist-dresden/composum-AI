document.addEventListener("DOMContentLoaded", () => {
  const list = document.querySelector(".model-list");
  if (!list) return;

  // Optional enhancement: save scroll position
  const scrollPos = localStorage.getItem("modelListScroll") || 0;
  list.scrollTop = scrollPos;

  window.addEventListener("beforeunload", () => {
    localStorage.setItem("modelListScroll", list.scrollTop);
  });
});