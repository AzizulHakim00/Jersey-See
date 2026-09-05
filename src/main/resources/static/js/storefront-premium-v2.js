(() => {
    "use strict";

    const reducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;

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
        previous?.addEventListener("click", () => track.scrollBy({left: -step(), behavior: reducedMotion ? "auto" : "smooth"}));
        next?.addEventListener("click", () => track.scrollBy({left: step(), behavior: reducedMotion ? "auto" : "smooth"}));
        track.addEventListener("scroll", updateControls, {passive: true});

        if ("ResizeObserver" in window) {
            new ResizeObserver(updateControls).observe(track);
        } else {
            window.addEventListener("resize", updateControls, {passive: true});
        }
        updateControls();
    });

    const revealNodes = [...document.querySelectorAll("[data-premium-reveal]")];
    if (reducedMotion || !("IntersectionObserver" in window)) {
        revealNodes.forEach((node) => node.classList.add("is-revealed"));
    } else if (revealNodes.length > 0) {
        const observer = new IntersectionObserver((entries) => {
            entries.forEach((entry) => {
                if (!entry.isIntersecting) return;
                entry.target.classList.add("is-revealed");
                observer.unobserve(entry.target);
            });
        }, {rootMargin: "0px 0px -7% 0px", threshold: .08});
        revealNodes.forEach((node) => observer.observe(node));
    }

    document.querySelectorAll("[data-premium-press]").forEach((control) => {
        control.addEventListener("pointerdown", () => control.classList.add("is-pressed"));
        const release = () => control.classList.remove("is-pressed");
        control.addEventListener("pointerup", release);
        control.addEventListener("pointercancel", release);
        control.addEventListener("pointerleave", release);
    });
})();
