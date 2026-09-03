(() => {
    "use strict";

    document.documentElement.classList.add("js");

    const setExpanded = (button, expanded) => button?.setAttribute("aria-expanded", String(expanded));

    const menuButton = document.querySelector("[data-mobile-menu-toggle], [data-menu-toggle]");
    const siteNav = document.querySelector("[data-mobile-menu]");
    const closeMenu = () => {
        siteNav?.classList.remove("is-open");
        setExpanded(menuButton, false);
    };
    menuButton?.addEventListener("click", () => {
        const open = siteNav?.classList.toggle("is-open") ?? false;
        setExpanded(menuButton, open);
    });

    const sidebar = document.getElementById("staffSidebar");
    const sidebarButton = document.querySelector("[data-sidebar-toggle]");
    const sidebarScrim = document.querySelector("[data-sidebar-scrim]");
    const closeSidebar = () => {
        sidebar?.classList.remove("is-open");
        sidebarScrim?.classList.remove("is-open");
        setExpanded(sidebarButton, false);
    };
    sidebarButton?.addEventListener("click", () => {
        const open = !(sidebar?.classList.contains("is-open") ?? false);
        sidebar?.classList.toggle("is-open", open);
        sidebarScrim?.classList.toggle("is-open", open);
        setExpanded(sidebarButton, open);
    });
    sidebarScrim?.addEventListener("click", closeSidebar);

    document.querySelectorAll("[data-dismiss-alert]").forEach((button) => {
        button.addEventListener("click", () => button.closest("[role='status'], [role='alert']")?.remove());
    });

    document.querySelectorAll("[data-password-toggle]").forEach((button) => {
        button.addEventListener("click", () => {
            const input = document.getElementById(button.dataset.passwordToggle);
            if (!(input instanceof HTMLInputElement)) return;
            const reveal = input.type === "password";
            input.type = reveal ? "text" : "password";
            button.textContent = reveal ? "Hide" : "Show";
        });
    });

    document.querySelectorAll("[data-image-input]").forEach((input) => {
        input.addEventListener("change", () => {
            const file = input.files?.[0];
            const preview = document.querySelector("[data-image-preview]");
            if (!file || !(preview instanceof HTMLImageElement)) return;
            const source = URL.createObjectURL(file);
            preview.addEventListener("load", () => URL.revokeObjectURL(source), {once: true});
            preview.src = source;
        });
    });

    document.querySelectorAll("[data-qty-minus], [data-qty-plus]").forEach((button) => {
        button.addEventListener("click", () => {
            const control = button.closest(".quantity-control");
            const input = control?.querySelector("input[type='number']");
            if (!(input instanceof HTMLInputElement)) return;
            const minimum = Number(input.min || 1);
            const maximum = Number(input.max || Number.MAX_SAFE_INTEGER);
            const change = button.hasAttribute("data-qty-plus") ? 1 : -1;
            input.value = String(Math.min(maximum, Math.max(minimum, Number(input.value || minimum) + change)));
            input.dispatchEvent(new Event("change", {bubbles: true}));
        });
    });

    const printingSelect = document.querySelector("[data-printing-select]");
    const printingFields = document.querySelector("[data-printing-fields]");
    const updatePrintingFields = () => {
        if (!(printingSelect instanceof HTMLSelectElement) || !(printingFields instanceof HTMLElement)) return;
        const hasValidationErrors = printingFields.querySelector(".field-error") !== null;
        printingFields.hidden = !hasValidationErrors && (printingSelect.value === "NONE" || printingSelect.value === "");
    };
    printingSelect?.addEventListener("change", updatePrintingFields);
    updatePrintingFields();

    const transactionField = document.querySelector("[data-transaction-field]");
    const updateTransactionField = () => {
        if (!(transactionField instanceof HTMLElement)) return;
        const selected = document.querySelector("[data-payment-method]:checked");
        const value = selected instanceof HTMLInputElement ? selected.value : "";
        transactionField.classList.toggle("is-optional", value === "CASH" || value === "CASH_ON_DELIVERY" || !value);
    };
    document.querySelectorAll("[data-payment-method]").forEach((input) => input.addEventListener("change", updateTransactionField));
    updateTransactionField();

    document.querySelectorAll("[data-table-search]").forEach((input) => {
        input.addEventListener("input", () => {
            const table = document.getElementById(input.dataset.tableSearch);
            const term = input.value.trim().toLocaleLowerCase();
            table?.querySelectorAll("tbody tr").forEach((row) => {
                row.hidden = term !== "" && !row.textContent.toLocaleLowerCase().includes(term);
            });
        });
    });

    const filterButton = document.querySelector("[data-filter-toggle]");
    const filterCard = document.querySelector("[data-filter-card]");
    const filterScrim = document.querySelector("[data-filter-scrim]");
    const filterClose = document.querySelector("[data-filter-close]");
    if (filterButton instanceof HTMLButtonElement && filterCard instanceof HTMLElement) {
        filterCard.classList.add("is-collapsible");
        filterCard.classList.remove("is-open");
        setExpanded(filterButton, false);
        const closeFilters = () => {
            filterCard.classList.remove("is-open");
            filterScrim?.classList.remove("is-open");
            setExpanded(filterButton, false);
        };
        filterButton.addEventListener("click", () => {
            const open = filterCard.classList.toggle("is-open");
            filterScrim?.classList.toggle("is-open", open);
            setExpanded(filterButton, open);
            if (open) filterCard.querySelector("input, select, button, a")?.focus();
        });
        filterClose?.addEventListener("click", closeFilters);
        filterScrim?.addEventListener("click", closeFilters);
        document.addEventListener("keydown", (event) => {
            if (event.key === "Escape" && filterCard.classList.contains("is-open")) {
                closeFilters();
                filterButton.focus();
            }
        });
    }

    const logoutModal = document.querySelector("[data-logout-modal]");
    let lastFocused = null;
    const showModal = () => {
        if (!(logoutModal instanceof HTMLElement)) return;
        lastFocused = document.activeElement;
        logoutModal.hidden = false;
        logoutModal.querySelector("button")?.focus();
    };
    const hideModal = () => {
        if (!(logoutModal instanceof HTMLElement)) return;
        logoutModal.hidden = true;
        if (lastFocused instanceof HTMLElement) lastFocused.focus();
    };
    document.querySelectorAll("[data-logout-form]").forEach((form) => form.addEventListener("submit", (event) => {
        event.preventDefault();
        showModal();
    }));
    document.querySelectorAll("[data-close-modal]").forEach((button) => button.addEventListener("click", hideModal));
    logoutModal?.addEventListener("click", (event) => {
        if (event.target === logoutModal) hideModal();
    });

    document.querySelectorAll("form[data-confirm]").forEach((form) => {
        form.addEventListener("submit", (event) => {
            if (form.dataset.confirmed === "true") return;
            if (!window.confirm(form.dataset.confirm || "Continue?")) event.preventDefault();
        });
    });

    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape") {
            hideModal();
            closeSidebar();
            if (siteNav?.classList.contains("is-open")) {
                closeMenu();
                menuButton?.focus();
            }
        }
    });
})();
