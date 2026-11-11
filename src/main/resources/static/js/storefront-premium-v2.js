(() => {
    "use strict";

    const reducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)");

    document.querySelectorAll("[data-hero-showcase]").forEach((showcase) => {
        const slides = [...showcase.querySelectorAll("[data-hero-slide]")];
        if (slides.length < 2) return;

        let activeIndex = 0;
        let pointerInside = false;
        let timer;

        const showNext = () => {
            slides[activeIndex].hidden = true;
            activeIndex = (activeIndex + 1) % slides.length;
            slides[activeIndex].hidden = false;
        };

        const updateTimer = () => {
            window.clearInterval(timer);
            if (!reducedMotion.matches && !pointerInside && !document.hidden) {
                timer = window.setInterval(showNext, 5000);
            }
        };

        showcase.addEventListener("pointerenter", (event) => {
            if (event.pointerType === "touch") return;
            pointerInside = true;
            updateTimer();
        });
        showcase.addEventListener("pointerleave", () => {
            pointerInside = false;
            updateTimer();
        });
        document.addEventListener("visibilitychange", updateTimer);
        reducedMotion.addEventListener("change", updateTimer);
        updateTimer();
    });

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
