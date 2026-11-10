package br.com.mochiladeemergencia.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import br.com.mochiladeemergencia.app.domain.model.Categoria
import br.com.mochiladeemergencia.app.domain.model.TipoDocumento

/**
 * Perfil (ex.: "Casa", "Casa da praia"). Cada perfil tem sua própria mochila,
 * seus documentos e seu checklist.
 */
@Entity(tableName = "perfis")
data class PerfilEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val nome: String,
    /** Onde a mochila fica guardada (ex.: "Armário da entrada"). */
    val localizacao: String = "",
    val criadoEm: Long = System.currentTimeMillis()
)

/** Item da mochila. [validadeEpochDay] nulo significa item sem validade (não gera alerta). */
@Entity(
    tableName = "itens",
    foreignKeys = [
        ForeignKey(
            entity = PerfilEntity::class,
            parentColumns = ["id"],
            childColumns = ["perfilId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("perfilId"), Index("chaveChecklist")]
)
data class ItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val perfilId: Long,
    val nome: String,
    val categoria: String = Categoria.OUTROS.name,
    val quantidade: Double = 1.0,
    val unidade: String = "un",
    val validadeEpochDay: Long? = null,
    val local: String = "",
    val observacao: String = "",
    val marcadoComoFalta: Boolean = false,
    /** Item consumido/substituído: aguarda reposição (lista "Repor"). */
    val aRepor: Boolean = false,
    /** Nome do arquivo de foto no diretório privado do app (nulo = sem foto). */
    val fotoArquivo: String? = null,
    /** Chave da sugestão do checklist que originou o item (nulo para itens livres). */
    val chaveChecklist: String? = null,
    /** Forma de consumo informada pelo usuário (vazio = não informada). Só faz sentido em comida. */
    val formaConsumo: String = "",
    val criadoEm: Long = System.currentTimeMillis(),
    val atualizadoEm: Long = System.currentTimeMillis()
)

/** Documento salvo pelo usuário: link ou arquivo local escolhido por ele. */
@Entity(
    tableName = "documentos",
    foreignKeys = [
        ForeignKey(
            entity = PerfilEntity::class,
            parentColumns = ["id"],
            childColumns = ["perfilId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("perfilId")]
)
data class DocumentoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val perfilId: Long,
    val titulo: String,
    val tipo: String = TipoDocumento.LINK.name,
    /** Link http(s) ou URI de conteúdo (content://) de um arquivo escolhido pelo usuário. */
    val uri: String = "",
    val categoria: String = "",
    val observacao: String = "",
    /** Verdadeiro quando o conteúdo é um arquivo local disponível sem internet. */
    val arquivoLocal: Boolean = false,
    val criadoEm: Long = System.currentTimeMillis()
)

/** Sugestão editável do checklist de um perfil. */
@Entity(
    tableName = "checklist",
    foreignKeys = [
        ForeignKey(
            entity = PerfilEntity::class,
            parentColumns = ["id"],
            childColumns = ["perfilId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["perfilId", "chave"], unique = true)]
)
data class ChecklistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val perfilId: Long,
    val chave: String,
    val nome: String,
    val categoria: String = Categoria.OUTROS.name,
    val quantidadeBase: Double = 1.0,
    val unidade: String = "un",
    /** Quando verdadeiro, a quantidade sugerida é multiplicada pelo número de pessoas. */
    val porPessoa: Boolean = false,
    /** Dica curta e opcional exibida junto da sugestão (ex.: "prefira fósforos à prova d'água"). */
    val dica: String = "",
    val ordem: Int = 0,
    val oculto: Boolean = false,
    val personalizado: Boolean = false
)

/** Contato pessoal de emergência cadastrado pelo usuário (por perfil). */
@Entity(
    tableName = "contatos",
    foreignKeys = [
        ForeignKey(
            entity = PerfilEntity::class,
            parentColumns = ["id"],
            childColumns = ["perfilId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("perfilId")]
)
data class ContatoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val perfilId: Long,
    val nome: String,
    val telefone: String,
    val parentesco: String = "",
    val observacao: String = "",
    val criadoEm: Long = System.currentTimeMillis()
)
