(() => {
  const commonFallback = '/images/posters/default-performance.webp';

  document.querySelectorAll('img[data-poster-fallback]').forEach(image => {
    const applyFallback = () => {
      const requestedFallback = image.dataset.posterFallback || commonFallback;
      const nextFallback = image.dataset.fallbackApplied ? commonFallback : requestedFallback;

      if (image.dataset.commonFallbackApplied || image.src.endsWith(nextFallback)) return;

      if (image.dataset.fallbackApplied) {
        image.dataset.commonFallbackApplied = 'true';
      } else {
        image.dataset.fallbackApplied = 'true';
      }
      image.alt = '';
      image.src = nextFallback;
    };

    image.addEventListener('error', applyFallback);
    if (image.complete && image.naturalWidth === 0) applyFallback();
  });
})();
