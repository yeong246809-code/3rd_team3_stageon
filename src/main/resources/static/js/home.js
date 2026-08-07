(() => {
  const carousel = document.querySelector('[data-carousel]');

  if (carousel) {
    const slides = [...carousel.querySelectorAll('[data-slide]')];
    const dots = [...carousel.querySelectorAll('[data-dot]')];
    const timer = carousel.querySelector('[data-carousel-timer]');
    const previousButton = carousel.querySelector('[data-carousel-prev]');
    const nextButton = carousel.querySelector('[data-carousel-next]');
    const reduceMotion = matchMedia('(prefers-reduced-motion: reduce)').matches;
    const autoplayDelay = 5600;
    let active = Math.max(0, slides.findIndex(slide => slide.classList.contains('is-active')));
    let autoplayTimer;
    let paused = false;

    const render = () => {
      slides.forEach((slide, index) => {
        const offset = ((index - active + slides.length + 1) % slides.length) - 1;
        slide.classList.toggle('is-active', offset === 0);
        slide.dataset.position = offset < 0 ? 'prev' : offset > 0 ? 'next' : 'active';
        slide.setAttribute('aria-hidden', offset === 0 ? 'false' : 'true');
      });

      dots.forEach((dot, index) => dot.classList.toggle('is-active', index === active));
      timer?.classList.remove('is-running');

      if (timer) {
        void timer.offsetWidth;
        if (!reduceMotion && !paused && !document.hidden) timer.classList.add('is-running');
      }
    };

    const stop = () => {
      clearTimeout(autoplayTimer);
      autoplayTimer = undefined;
    };

    const move = direction => {
      active = (active + direction + slides.length) % slides.length;
      render();
    };

    const start = () => {
      stop();
      if (reduceMotion || paused || document.hidden) return;
      autoplayTimer = setTimeout(() => {
        move(1);
        start();
      }, autoplayDelay);
    };

    const moveManually = direction => {
      move(direction);
      start();
    };

    previousButton?.addEventListener('click', () => moveManually(-1));
    nextButton?.addEventListener('click', () => moveManually(1));
    carousel.addEventListener('mouseenter', () => {
      paused = true;
      stop();
      timer?.classList.remove('is-running');
    });
    carousel.addEventListener('mouseleave', () => {
      paused = false;
      render();
      start();
    });
    document.addEventListener('visibilitychange', () => {
      if (document.hidden) {
        stop();
        timer?.classList.remove('is-running');
      } else {
        render();
        start();
      }
    });

    render();
    start();
  }

  const rankingTabs = [...document.querySelectorAll('[data-ranking-tab]')];
  const rankingPanels = [...document.querySelectorAll('[role="tabpanel"]')];

  if (rankingTabs.length && rankingTabs.length === rankingPanels.length) {
    const activateRanking = (index, moveFocus = false) => {
      rankingTabs.forEach((tab, tabIndex) => {
        const active = tabIndex === index;
        tab.classList.toggle('is-active', active);
        tab.setAttribute('aria-selected', String(active));
        tab.tabIndex = active ? 0 : -1;
        rankingPanels[tabIndex].hidden = !active;
      });
      if (moveFocus) rankingTabs[index].focus();
    };

    rankingTabs.forEach((tab, index) => {
      tab.addEventListener('click', () => activateRanking(index));
      tab.addEventListener('keydown', event => {
        let nextIndex;
        if (event.key === 'ArrowRight') nextIndex = (index + 1) % rankingTabs.length;
        if (event.key === 'ArrowLeft') nextIndex = (index - 1 + rankingTabs.length) % rankingTabs.length;
        if (event.key === 'Home') nextIndex = 0;
        if (event.key === 'End') nextIndex = rankingTabs.length - 1;
        if (nextIndex === undefined) return;
        event.preventDefault();
        activateRanking(nextIndex, true);
      });
    });
  }

  const fogScene = document.querySelector('[data-fog-scene]');
  if (!fogScene) return;

  let ticking = false;
  const updateFog = () => {
    const rect = fogScene.getBoundingClientRect();
    const viewportCenter = innerHeight / 2;
    const sceneCenter = rect.top + rect.height / 2;
    const distance = Math.abs(sceneCenter - viewportCenter);
    const reveal = Math.max(0, 1 - distance / (innerHeight * 0.72));
    fogScene.style.setProperty('--reveal', reveal.toFixed(3));
    ticking = false;
  };

  addEventListener('scroll', () => {
    if (ticking) return;
    requestAnimationFrame(updateFog);
    ticking = true;
  }, { passive: true });
  addEventListener('resize', updateFog);
  updateFog();
})();
