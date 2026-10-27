import { Link } from 'react-router-dom'
import { LegalPage } from '../components/LegalPage'

export function CommunityPage() {
  return (
    <LegalPage title="Normas de la comunidad">
      <p>
        Estas normas complementan los <Link to="/terminos">Términos y Condiciones</Link> y buscan que
        el Foro de Cultivadores sea un espacio útil, respetuoso y seguro para compartir conocimiento.
      </p>

      <h2>Lo que buscamos</h2>
      <ul>
        <li>Respeto y buena fe: debatí ideas, no agredas personas.</li>
        <li>Contenido educativo y experiencias reales de cultivo.</li>
        <li>Aportes claros: buenos títulos, contexto y fotos que ayuden.</li>
      </ul>

      <h2>Lo que no está permitido</h2>
      <ul>
        <li>Comprar, vender u ofrecer sustancias, semillas o esquejes.</li>
        <li>Apología de actividades ilegales o instrucciones para cometerlas.</li>
        <li>Acoso, insultos, discriminación o amenazas.</li>
        <li>Spam, autopromoción abusiva o publicidad no autorizada.</li>
        <li>Publicar datos personales de terceros o suplantar identidades.</li>
      </ul>

      <h2>Marco legal (descargo)</h2>
      <p>
        El contenido es informativo. Cada persona es responsable de cumplir la legislación vigente en
        su jurisdicción respecto del cultivo y uso. El Sitio no promueve conductas ilícitas.
      </p>

      <h2>Moderación y reportes</h2>
      <p>
        El equipo de moderación puede editar, ocultar o eliminar contenido y{' '}
        <strong>suspender o dar de baja</strong> cuentas que incumplan estas normas. Si ves algo que
        las viola, avisá a la moderación. Las reincidencias o faltas graves pueden derivar en la baja
        de la cuenta.
      </p>
    </LegalPage>
  )
}
