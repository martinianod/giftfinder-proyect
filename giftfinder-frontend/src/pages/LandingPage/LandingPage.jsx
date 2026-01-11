import { useNavigate } from "react-router-dom";
import styles from "./LandingPage.module.css";

export default function LandingPage() {
  const navigate = useNavigate();

  return (
    <div className={styles.page}>
      
      {/* ================== HERO ================== */}
      <section className={styles.hero}>
        <div className={styles.heroContent}>

          <h1 className={styles.title}>
            Encontrá lo que <span className={styles.gradient}>aún no sabías que querías.</span>
          </h1>

          <p className={styles.subtitle}>
            Findora es tu asistente inteligente de regalos y compras.  
            Usá texto o voz, y nuestra IA te muestra opciones precisas de múltiples tiendas.
          </p>

          <button
            className={styles.primaryButton}
            onClick={() => navigate("/app")}
          >
            🚀 Probar Findora ahora
          </button>

          <button
            className={styles.secondaryButton}
            onClick={() => {
              const el = document.getElementById("features");
              el && el.scrollIntoView({ behavior: "smooth" });
            }}
          >
            Ver cómo funciona ↓
          </button>
        </div>
      </section>

      {/* ================== FEATURES ================== */}
      <section id="features" className={styles.featuresSection}>
        <h2 className={styles.sectionTitle}>Cómo funciona Findora</h2>

        <div className={styles.featuresGrid}>
          <Feature
            step="01"
            title="Contanos a quién le regalás"
            description="Edad, relación, gustos, ocasión y presupuesto. Podés hablar o escribir."
          />
          <Feature
            step="02"
            title="La IA filtra miles de productos"
            description="Encuentra opciones precisas y relevantes entre múltiples tiendas."
          />
          <Feature
            step="03"
            title="Aprende de tus gustos"
            description="Cuanto más la usás, mejores son las sugerencias."
          />
        </div>
      </section>

      {/* ================== BENEFICIOS ================== */}
      <section className={styles.benefitsSection}>
        <h2 className={styles.sectionTitle}>¿Por qué elegir Findora?</h2>

        <div className={styles.benefitsGrid}>
          <Benefit
            title="Menos tiempo buscando"
            description="Una sola búsqueda, y Findora hace el trabajo por vos."
          />
          <Benefit
            title="Regalos que sorprenden"
            description="Sugerencias realmente pensadas para la persona."
          />
          <Benefit
            title="Perfecto para ofertas"
            description="Compara precios y encuentra oportunidades."
          />
          <Benefit
            title="También para vos"
            description="Descubrí productos alineados a tu propio estilo."
          />
        </div>
      </section>

      {/* ================== CASOS DE USO ================== */}
      <section className={styles.useCasesSection}>
        <h2 className={styles.sectionTitle}>Casos reales</h2>

        <div className={styles.useCasesGrid}>
          <UseCase
            title="Regalo para pareja"
            text="“Regalo para mi novia de 27, le gustan los libros y el diseño, hasta $40.000.”"
          />
          <UseCase
            title="Día de la madre"
            text="“Algo especial, útil y con presupuesto de $60.000.”"
          />
          <UseCase
            title="Para mí"
            text="“Quiero algo minimalista, tech y útil para todos los días.”"
          />
        </div>
      </section>

      {/* ================== FOOTER ================== */}
      <footer className={styles.footer}>
        <p>© {new Date().getFullYear()} Findora · Tu asistente inteligente de regalos</p>
        <button className={styles.footerLink} onClick={() => navigate("/app")}>
          Ir a la app →
        </button>
      </footer>
    </div>
  );
}

function Feature({ step, title, description }) {
  return (
    <div className={styles.featureCard}>
      <span className={styles.featureStep}>{step}</span>
      <h3 className={styles.featureTitle}>{title}</h3>
      <p className={styles.featureDescription}>{description}</p>
    </div>
  );
}

function Benefit({ title, description }) {
  return (
    <div className={styles.benefitCard}>
      <h3 className={styles.benefitTitle}>{title}</h3>
      <p className={styles.benefitDescription}>{description}</p>
    </div>
  );
}

function UseCase({ title, text }) {
  return (
    <div className={styles.useCaseCard}>
      <h3 className={styles.useCaseTitle}>{title}</h3>
      <p className={styles.useCaseText}>{text}</p>
    </div>
  );
}
