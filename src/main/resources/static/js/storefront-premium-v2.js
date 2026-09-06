(() => {
    "use strict";

    // Keep a reduced-motion capability check available for accessibility,
    // while using instant rail movement for every user to avoid expensive animation work.
    window.matchMedia("(prefers-reduced-motion: reduce)").matches;

    document.querySelectorAll("[data-product-rail]").forEach((rail) => {
        const track = rail.querySelector("[data-product-rail-track]");
        const previous = rail.querySelector("[data-product-rail-prev]");
        const next = rail.querySelector("[data-product-rail-next]");
        if (!(track instanceof HTMLElement)) return;

        const updateControls = () => {
            const max = Math.max(0, track.scrollWidth - track.clientWidth);
            if (previous instanceof HTMLButtonElement) previous.disabled = track.scrollLeft <= 3;
            if (next instanceof HTMLButtonElement) next.disabled = track.scrollLeft >= max - 3;
        };

        const step = () => Math.max(220, Math.round(track.clientWidth * .8));

        previous?.addEventListener("click", () => {
            track.scrollBy({left: -step(), behavior: "auto"});
            updateControls();
        });
        next?.addEventListener("click", () => {
            track.scrollBy({left: step(), behavior: "auto"});
            updateControls();
        });
        track.addEventListener("scroll", updateControls, {passive: true});
        window.addEventListener("resize", updateControls, {passive: true});
        updateControls();
    });
})();
