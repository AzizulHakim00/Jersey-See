(() => {
    "use strict";

    const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;

    document.querySelectorAll("[data-product-rail]").forEach((rail) => {
        const track = rail.querySelector("[data-product-rail-track]");
        const previous = rail.querySelector("[data-product-rail-prev]");
        const next = rail.querySelector("[data-product-rail-next]");
        if (!(track instanceof HTMLElement)) return;

        const amount = () => {
            const card = track.querySelector("[data-commerce-card]");
            const cardWidth = card instanceof HTMLElement ? card.getBoundingClientRect().width : track.clientWidth * .8;
            const gap = Number.parseFloat(window.getComputedStyle(track).columnGap || window.getComputedStyle(track).gap || "16") || 16;
            return Math.max(cardWidth + gap, track.clientWidth * .72);
        };

        const update = () => {
            const maxScroll = Math.max(0, track.scrollWidth - track.clientWidth - 2);
            if (previous instanceof HTMLButtonElement) previous.disabled = track.scrollLeft <= 2;
            if (next instanceof HTMLButtonElement) next.disabled = track.scrollLeft >= maxScroll;
        };

        const move = (direction) => {
            track.scrollBy({left: direction * amount(), behavior: reduceMotion ? "auto" : "smooth"});
        };

        previous?.addEventListener("click", () => move(-1));
        next?.addEventListener("click", () => move(1));
        track.addEventListener("scroll", update, {passive: true});
        window.addEventListener("resize", update, {passive: true});
        update();
    });
})();
