import { Link } from 'react-router-dom'
import { LegalPage } from '../components/LegalPage'

// Página completa.
export function PrivacyPage() {
  return (
    <LegalPage title="Política de Privacidad" updated="27 de octubre de 2026">
      <PrivacyContent />
    </LegalPage>
  )
}

// Solo el contenido (reutilizado en el modal del registro).
export function PrivacyContent() {
  return (
    <>
      <p>
        Esta Política explica qué datos personales tratamos y cómo, conforme a la{' '}
        <strong>Ley 25.326 de Protección de los Datos Personales</strong> de la República Argentina.
      </p>

      <h2>1. Responsable de la base de datos</h2>
      <p>
        Responsable: <strong>[nombre del responsable]</strong>. Contacto:{' '}
        <strong>[email de contacto]</strong>. Domicilio: <strong>[localidad, Argentina]</strong>.
      </p>

      <h2>2. Qué datos recabamos</h2>
      <ul>
        <li>
          <strong>De registro:</strong> nombre de usuario, email y contraseña (guardada siempre
          cifrada / con hash, nunca en texto plano).
        </li>
        <li>
          <strong>De perfil (opcionales):</strong> biografía, imágenes de avatar y portada, y datos
          de comercio (sitio web, teléfono, Instagram, ubicación) si cargás un perfil de growshop.
        </li>
        <li>
          <strong>Que publicás:</strong> posts, comentarios e imágenes.
        </li>
        <li>
          <strong>Técnicos:</strong> una cookie de sesión para mantenerte identificado/a.
        </li>
      </ul>

      <h2>3. Para qué los usamos</h2>
      <p>
        Para crear y gestionar tu cuenta, permitirte publicar e interactuar, verificar tu email,
        recuperar tu contraseña, moderar la comunidad y mantener la seguridad del Sitio. No vendemos
        tus datos personales.
      </p>

      <h2>4. Base legal / consentimiento</h2>
      <p>
        El tratamiento se basa en tu <strong>consentimiento</strong> (que prestás al registrarte y
        aceptar esta Política) y en la ejecución de la relación que nace al usar el Sitio.
      </p>

      <h2>5. Tus derechos</h2>
      <p>
        Podés ejercer en forma gratuita tus derechos de <strong>acceso, rectificación,
        actualización y supresión</strong> de tus datos escribiendo a{' '}
        <strong>[email de contacto]</strong>. La Agencia de Acceso a la Información Pública (AAIP),
        órgano de control de la Ley 25.326, atiende denuncias por violaciones a la normativa.
      </p>

      <h2>6. Cómo eliminar tu cuenta</h2>
      <p>
        Podés darte de baja vos mismo/a desde <Link to="/me">Mi perfil</Link> → “Eliminar mi cuenta”.
        Al hacerlo <strong>anonimizamos tus datos personales</strong> (email, biografía, imágenes y
        datos de comercio) y la cuenta deja de poder iniciar sesión. Para no romper las conversaciones,
        tus posts y comentarios se conservan mostrados como “[cuenta eliminada]”. Si preferís que
        también se eliminen esos contenidos, escribinos a <strong>[email de contacto]</strong>.
      </p>

      <h2>7. Cookies</h2>
      <p>
        Usamos una <strong>cookie de sesión</strong> estrictamente necesaria para mantener tu inicio
        de sesión. No usamos cookies de publicidad ni de seguimiento de terceros.
      </p>

      <h2>8. Conservación y seguridad</h2>
      <p>
        Conservamos tus datos mientras tu cuenta esté activa. Aplicamos medidas razonables de
        seguridad (contraseñas con hash, transporte cifrado). Ningún sistema es 100% infalible, pero
        trabajamos para proteger tu información.
      </p>

      <h2>9. Menores y terceros</h2>
      <p>
        El Sitio no está dirigido a menores de 18 años. Respecto de growshops y terceros, revisá el
        descargo de los <Link to="/terminos">Términos y Condiciones</Link>.
      </p>

      <h2>10. Cambios</h2>
      <p>
        Podemos actualizar esta Política; publicaremos la versión vigente en esta página.
      </p>
    </>
  )
}
