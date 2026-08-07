const THREE = window.THREE;

const fogScene = document.querySelector('[data-fog-scene]');
const canvas = document.querySelector('[data-three-fog]');

if (fogScene && canvas && THREE) {
  const vertexShader = `
    varying vec2 vUv;

    void main() {
      vUv = uv;
      gl_Position = vec4(position, 1.0);
    }
  `;

  const fragmentShader = `
    precision highp float;

    uniform float uTime;
    uniform float uReveal;
    uniform vec2 uResolution;
    varying vec2 vUv;

    float hash(vec2 p) {
      return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
    }

    float noise(vec2 p) {
      vec2 i = floor(p);
      vec2 f = fract(p);
      f = f * f * (3.0 - 2.0 * f);
      return mix(
        mix(hash(i), hash(i + vec2(1.0, 0.0)), f.x),
        mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), f.x),
        f.y
      );
    }

    float fbm(vec2 p) {
      float value = 0.0;
      float amplitude = 0.55;
      mat2 turn = mat2(0.84, -0.54, 0.54, 0.84);

      for (int i = 0; i < 5; i++) {
        value += amplitude * noise(p);
        p = turn * p * 2.03 + 13.17;
        amplitude *= 0.5;
      }

      return value;
    }

    void main() {
      float aspect = uResolution.x / max(uResolution.y, 1.0);
      float boundaryNoise = noise(vec2(vUv.y * 2.8 - uTime * 0.012, uTime * 0.009 + 23.4));
      float boundaryRipple = sin(vUv.y * 9.0 + uTime * 0.16) * 0.014;
      float driftingCenter = 0.5 + (boundaryNoise - 0.5) * 0.13 + boundaryRipple;
      float sideBlend = smoothstep(driftingCenter - 0.16, driftingCenter + 0.16, vUv.x);
      float direction = mix(-1.0, 1.0, sideBlend);
      float distanceFromCenter = abs(vUv.x - driftingCenter);
      vec2 p = vec2((vUv.x - 0.5) * aspect + 0.5, vUv.y);

      vec2 backFlow = p;
      backFlow.x -= direction * uReveal * 0.13;
      backFlow += vec2(direction * uTime * 0.010, -uTime * 0.014);

      vec2 middleFlow = p * vec2(1.38, 1.16);
      middleFlow.x -= direction * uReveal * 0.27;
      middleFlow += vec2(-direction * uTime * 0.018, uTime * 0.010);

      vec2 frontFlow = p * vec2(2.08, 1.72);
      frontFlow.x -= direction * uReveal * 0.42;
      frontFlow += vec2(direction * uTime * 0.027, -uTime * 0.021);

      vec2 threadFlow = p * vec2(3.4, 2.2);
      threadFlow.x -= direction * uReveal * 0.58;
      threadFlow += vec2(-direction * uTime * 0.036, uTime * 0.018);

      float back = fbm(backFlow * 2.2 + 4.0);
      float middle = fbm(middleFlow * 2.0 + vec2(17.0, 3.0));
      float front = fbm(frontFlow * 1.65 + vec2(7.0, 19.0));
      float threads = fbm(threadFlow * 1.25 + vec2(29.0, 11.0));
      float layeredFog = back * 0.34 + middle * 0.29 + front * 0.25 + threads * 0.18;

      float edgeVariation = (noise(vec2(vUv.y * 5.2 + uTime * 0.01, 41.7)) - 0.5) * 0.07;
      float openedSides = smoothstep(0.08 + edgeVariation, 0.47 + edgeVariation, distanceFromCenter);
      float openCenter = mix(0.34, 1.0, openedSides);
      float splitMask = mix(1.0, openCenter, uReveal);
      float verticalFade = smoothstep(0.0, 0.08, vUv.y) * (1.0 - smoothstep(0.94, 1.0, vUv.y));
      float cloudyAlpha = smoothstep(0.23, 0.75, layeredFog);
      float broadNoise = back * 0.62 + middle * 0.38;
      float broadHaze = mix(0.34, 0.18, uReveal) * (0.7 + broadNoise * 0.46);
      float alpha = clamp((cloudyAlpha * splitMask * 0.78 + broadHaze) * verticalFade, 0.0, 0.92);

      vec3 shadowFog = vec3(0.38, 0.43, 0.51);
      vec3 pearlFog = vec3(0.86, 0.88, 0.92);
      vec3 fogColor = mix(shadowFog, pearlFog, smoothstep(0.38, 0.76, layeredFog));
      gl_FragColor = vec4(fogColor, alpha);
    }
  `;

  try {
    const renderer = new THREE.WebGLRenderer({
      canvas,
      alpha: true,
      antialias: false,
      premultipliedAlpha: false,
      powerPreference: 'high-performance'
    });
    renderer.setClearColor(0x000000, 0);
    renderer.setPixelRatio(Math.min(devicePixelRatio, 1.5));

    const scene = new THREE.Scene();
    const camera = new THREE.OrthographicCamera(-1, 1, 1, -1, 0, 1);
    camera.position.z = 1;
    const uniforms = {
      uTime: { value: 0 },
      uReveal: { value: 0 },
      uResolution: { value: new THREE.Vector2(1, 1) }
    };
    const material = new THREE.ShaderMaterial({
      uniforms,
      vertexShader,
      fragmentShader,
      transparent: true,
      depthTest: false,
      depthWrite: false
    });
    const plane = new THREE.Mesh(new THREE.PlaneGeometry(2, 2), material);
    scene.add(plane);

    const reduceMotion = matchMedia('(prefers-reduced-motion: reduce)').matches;
    let visible = false;
    let animationFrame = 0;
    let startTime = performance.now();

    const resize = () => {
      const width = Math.max(1, canvas.clientWidth);
      const height = Math.max(1, canvas.clientHeight);
      renderer.setSize(width, height, false);
      uniforms.uResolution.value.set(canvas.width, canvas.height);
    };

    const draw = now => {
      animationFrame = 0;
      resize();
      uniforms.uReveal.value = Number.parseFloat(fogScene.style.getPropertyValue('--reveal')) || 0;
      uniforms.uTime.value = reduceMotion ? 0 : (now - startTime) * 0.001;
      renderer.render(scene, camera);

      if (visible && fogScene.dataset.fogMode === 'webgl' && !reduceMotion) {
        animationFrame = requestAnimationFrame(draw);
      }
    };

    const requestDraw = () => {
      if (!animationFrame && visible && fogScene.dataset.fogMode === 'webgl') {
        animationFrame = requestAnimationFrame(draw);
      }
    };

    const observer = new IntersectionObserver(entries => {
      visible = entries[0].isIntersecting;
      if (visible) requestDraw();
      else if (animationFrame) {
        cancelAnimationFrame(animationFrame);
        animationFrame = 0;
      }
    }, { rootMargin: '120px 0px' });

    observer.observe(fogScene);
    addEventListener('scroll', requestDraw, { passive: true });
    addEventListener('resize', requestDraw);
    document.addEventListener('visibilitychange', () => {
      if (document.hidden && animationFrame) {
        cancelAnimationFrame(animationFrame);
        animationFrame = 0;
      } else {
        startTime = performance.now() - uniforms.uTime.value * 1000;
        requestDraw();
      }
    });

    fogScene.classList.add('is-webgl-ready');
  } catch (error) {
    console.warn('Three.js fog could not start. Falling back to the static fog layer.', error);
    fogScene.classList.add('is-webgl-unavailable');
  }
} else if (fogScene) {
  fogScene.classList.add('is-webgl-unavailable');
}
