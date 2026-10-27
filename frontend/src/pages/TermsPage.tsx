import { Link } from 'react-router-dom'
import { LegalPage } from '../components/LegalPage'

// Página completa.
export function TermsPage() {
  return (
    <LegalPage title="Términos y Condiciones" updated="27 de octubre de 2026">
      <TermsContent />
    </LegalPage>
  )
}

// Solo el contenido (reutilizado en el modal del registro).
export function TermsContent() {
  return (
    <>
      <p>
        Bienvenido/a al Foro de Cultivadores (el “Sitio”). Al registrarte o usar el Sitio aceptás
        estos Términos y Condiciones y nuestra{' '}
        <Link to="/privacidad">Política de Privacidad</Link>. Si no estás de acuerdo, no uses el Sitio.
      </p>

      <h2>1. Mayoría de edad</h2>
      <p>
        El Sitio está destinado <strong>exclusivamente a personas mayores de 18 años</strong>. Al
        registrarte declarás y garantizás que tenés al menos 18 años. Si detectamos cuentas de
        menores, las daremos de baja.
      </p>

      <h2>2. Qué es el Sitio</h2>
      <p>
        El Foro de Cultivadores es una comunidad con fines <strong>informativos y educativos</strong>{' '}
        sobre cultivo. El contenido es generado por las personas usuarias y no constituye
        asesoramiento profesional, médico, legal ni agronómico.
      </p>

      <h2>3. Responsabilidad sobre tus publicaciones</h2>
      <p>
        Sos el/la único/a responsable del contenido que publicás (textos, imágenes, comentarios y
        datos de tu perfil). Al publicar, declarás que tenés derecho a hacerlo y que el contenido no
        infringe leyes ni derechos de terceros. Debés cumplir la legislación vigente en tu
        jurisdicción; el Sitio no promueve ni habilita actividades ilícitas.
      </p>

      <h2>4. Conducta prohibida</h2>
      <p>En el Sitio no se permite:</p>
      <ul>
        <li>Comprar, vender, ofrecer o facilitar la comercialización de sustancias o semillas.</li>
        <li>Publicar contenido ilegal, apología del delito, violento, discriminatorio o de acoso.</li>
        <li>Spam, estafas, publicidad no autorizada o enlaces maliciosos.</li>
        <li>Suplantar identidades o publicar datos personales de terceros sin consentimiento.</li>
        <li>Vulnerar la seguridad del Sitio o de otras personas usuarias.</li>
      </ul>

      <h2>5. Moderación, suspensión y baja</h2>
      <p>
        Podemos editar, ocultar o eliminar contenido, y <strong>suspender o dar de baja</strong>{' '}
        cuentas que incumplan estos Términos o las{' '}
        <Link to="/normas">Normas de la comunidad</Link>, especialmente ante: publicación de
        contenido prohibido, reincidencia tras advertencias, fraude o suplantación, o cualquier uso
        que ponga en riesgo a la comunidad o al Sitio. En casos graves la baja puede ser inmediata y
        sin aviso previo.
      </p>

      <h2>6. Contenido de terceros y growshops (descargo)</h2>
      <p>
        El Sitio puede incluir perfiles de comercios (“growshops”), enlaces y recomendaciones
        publicadas por terceros. <strong>No avalamos, verificamos ni nos responsabilizamos</strong>{' '}
        por los productos, servicios, precios, consejos o afirmaciones de terceros. Cualquier
        relación o transacción que inicies con ellos es por tu cuenta y riesgo.
      </p>

      <h2>7. Contenido que publicás</h2>
      <p>
        Conservás la titularidad de lo que publicás. Al publicarlo, nos otorgás una licencia no
        exclusiva y gratuita para mostrarlo y distribuirlo dentro del Sitio con el fin de operarlo.
        Podés eliminar tu contenido dándote de baja (ver la{' '}
        <Link to="/privacidad">Política de Privacidad</Link>).
      </p>

      <h2>8. Limitación de responsabilidad</h2>
      <p>
        El Sitio se ofrece “tal cual”, sin garantías de disponibilidad ni de exactitud del contenido.
        En la máxima medida permitida por la ley, no seremos responsables por daños derivados del uso
        del Sitio o de decisiones tomadas en base a su contenido.
      </p>

      <h2>9. Cambios en los Términos</h2>
      <p>
        Podemos actualizar estos Términos. Publicaremos la versión vigente en esta página; el uso
        continuado del Sitio implica la aceptación de los cambios.
      </p>

      <h2>10. Ley aplicable y contacto</h2>
      <p>
        Estos Términos se rigen por las leyes de la República Argentina. Ante cualquier consulta,
        escribinos a <strong>[email de contacto]</strong>. Responsable: <strong>[nombre del
        responsable]</strong>.
      </p>
    </>
  )
}
