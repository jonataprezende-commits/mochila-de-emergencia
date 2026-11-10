package br.com.mochiladeemergencia.app.ui.icones

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Ícones próprios em vetor (24x24), alinhados ao Material Design.
 * Definidos aqui para manter o app leve: evita a dependência
 * `material-icons-extended`, que adiciona milhares de classes ao APK.
 */
object Icones {

    val Mochila: ImageVector by lazy {
        criar("Mochila") {
            preenchido(
                "M7,5 H17 A4,4 0 0 1 21,9 V20 A2,2 0 0 1 19,22 H5 A2,2 0 0 1 3,20 V9 " +
                    "A4,4 0 0 1 7,5 Z " +
                    "M9,14 H15 A1,1 0 0 1 16,15 V18 A1,1 0 0 1 15,19 H9 A1,1 0 0 1 8,18 V15 " +
                    "A1,1 0 0 1 9,14 Z",
                regra = PathFillType.EvenOdd
            )
            traco("M9,5 A3,3 0 0 1 15,5", 1.8f)
        }
    }

    val Checklist: ImageVector by lazy {
        criar("Checklist") {
            traco(
                "M3,7 L4.7,8.7 L8,5.4 M10.5,7 H21 " +
                    "M3,12.5 L4.7,14.2 L8,10.9 M10.5,12.5 H21 " +
                    "M3,18 L4.7,19.7 L8,16.4 M10.5,18 H21",
                2f
            )
        }
    }

    val Documento: ImageVector by lazy {
        criar("Documento") {
            preenchido("M14,2 H6 A2,2 0 0 0 4,4 V20 A2,2 0 0 0 6,22 H18 A2,2 0 0 0 20,20 V8 Z")
            traco("M8,12 H16 M8,16 H14", 1.8f)
        }
    }

    val DocumentoPdf: ImageVector by lazy {
        criar("DocumentoPdf") {
            preenchido("M14,2 H6 A2,2 0 0 0 4,4 V20 A2,2 0 0 0 6,22 H18 A2,2 0 0 0 20,20 V8 Z")
            traco("M8,11 H16 M8,14.5 H14", 1.6f)
            preenchido("M8,17 H16 A1,1 0 0 1 16,19 H8 A1,1 0 0 1 8,17 Z")
        }
    }

    val Imagem: ImageVector by lazy {
        criar("Imagem") {
            preenchido(
                "M4,3 H20 A1,1 0 0 1 21,4 V20 A1,1 0 0 1 20,21 H4 A1,1 0 0 1 3,20 V4 " +
                    "A1,1 0 0 1 4,3 Z M5,5 V19 H19 V5 Z",
                regra = PathFillType.EvenOdd
            )
            preenchido("M6,18 L10,11.5 L13.5,16 L16,13 L19,18 Z")
            preenchido("M8.5,7 A1.6,1.6 0 1 0 8.5,10.2 A1.6,1.6 0 1 0 8.5,7 Z")
        }
    }

    val Video: ImageVector by lazy {
        criar("Video") {
            preenchido(
                "M3,5 H21 A1,1 0 0 1 22,6 V18 A1,1 0 0 1 21,19 H3 A1,1 0 0 1 2,18 V6 " +
                    "A1,1 0 0 1 3,5 Z M4,7 V17 H20 V7 Z",
                regra = PathFillType.EvenOdd
            )
            preenchido("M10,9 L16,12 L10,15 Z")
        }
    }

    val Link: ImageVector by lazy {
        criar("Link") {
            traco(
                "M10.2,13.8 L13.8,10.2 " +
                    "M8.4,16.4 A4,4 0 0 1 8.4,10.8 L10.4,8.8 A4,4 0 0 1 14.4,10.8 " +
                    "M15.6,7.6 A4,4 0 0 1 15.6,13.2 L13.6,15.2 A4,4 0 0 1 9.6,13.2",
                1.9f
            )
        }
    }

    val Upload: ImageVector by lazy {
        criar("Upload") {
            preenchido("M12,3 L17.5,8.5 H14 V15 H10 V8.5 H6.5 Z")
            traco("M4,17 V19.5 A1.5,1.5 0 0 0 5.5,21 H18.5 A1.5,1.5 0 0 0 20,19.5 V17", 2f)
        }
    }

    val Download: ImageVector by lazy {
        criar("Download") {
            preenchido("M12,21 L6.5,15.5 H10 V9 H14 V15.5 H17.5 Z")
            traco("M4,7 V4.5 A1.5,1.5 0 0 1 5.5,3 H18.5 A1.5,1.5 0 0 1 20,4.5 V7", 2f)
        }
    }

    val AbrirExterno: ImageVector by lazy {
        criar("AbrirExterno") {
            preenchido("M14,3 V5 H18.6 L11,12.6 L12.4,14 L20,6.4 V11 H22 V3 Z")
            preenchido("M5,5 H11 V7 H7 V19 H17 V15 H19 V19 A2,2 0 0 1 17,21 H5 A2,2 0 0 1 3,19 V7 A2,2 0 0 1 5,5 Z")
        }
    }

    val NotificacoesOff: ImageVector by lazy {
        criar("NotificacoesOff") {
            preenchido(
                "M12,22 A2,2 0 0 0 14,20 H10 A2,2 0 0 0 12,22 Z " +
                    "M18,16 V11 A6,6 0 0 0 13.5,5.2 V4 A1.5,1.5 0 0 0 10.5,4 V5.2 " +
                    "A6,6 0 0 0 6,11 V16 L4,18 V19 H20 V18 Z"
            )
            traco("M3.5,3.5 L20.5,20.5", 2.2f)
        }
    }

    /** Cruz de emergência (aba Emergência e primeiros socorros). */
    val Cruz: ImageVector by lazy {
        criar("Cruz") {
            preenchido(
                "M9.5,3 H14.5 A1.5,1.5 0 0 1 16,4.5 V8 H19.5 A1.5,1.5 0 0 1 21,9.5 V14.5 " +
                    "A1.5,1.5 0 0 1 19.5,16 H16 V19.5 A1.5,1.5 0 0 1 14.5,21 H9.5 " +
                    "A1.5,1.5 0 0 1 8,19.5 V16 H4.5 A1.5,1.5 0 0 1 3,14.5 V9.5 " +
                    "A1.5,1.5 0 0 1 4.5,8 H8 V4.5 A1.5,1.5 0 0 1 9.5,3 Z"
            )
        }
    }

    /** Telefone para os botões "Ligar" (abre o discador, não liga sozinho). */
    val Telefone: ImageVector by lazy {
        criar("Telefone") {
            preenchido(
                "M6.6,2.5 A1.6,1.6 0 0 1 8.2,3.6 L9.1,6.7 A1.6,1.6 0 0 1 8.6,8.4 L7.4,9.6 " +
                    "A12,12 0 0 0 14.4,16.6 L15.6,15.4 A1.6,1.6 0 0 1 17.3,14.9 L20.4,15.8 " +
                    "A1.6,1.6 0 0 1 21.5,17.4 V20 A1.7,1.7 0 0 1 19.7,21.7 " +
                    "C10.7,21.7 2.3,13.3 2.3,4.3 A1.7,1.7 0 0 1 4,2.5 Z"
            )
        }
    }

    /** Localização (pin) para o card da localização da mochila. */
    val Localizacao: ImageVector by lazy {
        criar("Localizacao") {
            preenchido(
                "M12,2 A7.5,7.5 0 0 1 19.5,9.5 C19.5,15 12,22 12,22 C12,22 4.5,15 4.5,9.5 " +
                    "A7.5,7.5 0 0 1 12,2 Z M12,6.6 A2.9,2.9 0 1 0 12,12.4 A2.9,2.9 0 1 0 12,6.6 Z",
                regra = PathFillType.EvenOdd
            )
        }
    }

    /** Auditória / conferir (prancheta com check). */
    val Conferir: ImageVector by lazy {
        criar("Conferir") {
            preenchido(
                "M8,2 H16 A1,1 0 0 1 17,3 V5 H7 V3 A1,1 0 0 1 8,2 Z " +
                    "M5,4 H7 V6.5 A1,1 0 0 0 8,7.5 H16 A1,1 0 0 0 17,6.5 V4 H19 " +
                    "A1.5,1.5 0 0 1 20.5,5.5 V20.5 A1.5,1.5 0 0 1 19,22 H5 " +
                    "A1.5,1.5 0 0 1 3.5,20.5 V5.5 A1.5,1.5 0 0 1 5,4 Z"
            )
            traco("M7,12.5 L9,14.5 L11,11 M13,12 H17 M7,17 L9,19 L11,15.5 M13,17.5 H17", 1.6f)
        }
    }

    /** Repor / recarregar. */
    val Repor: ImageVector by lazy {
        criar("Repor") {
            traco("M20,12 A8,8 0 1 1 17.6,6.4 M20,4 V9 H15", 2f)
            traco("M12,8 V12.5 L15,14.5", 1.8f)
        }
    }

    /** Câmera para a foto do item. */
    val Camera: ImageVector by lazy {
        criar("Camera") {
            preenchido(
                "M9,4 L10.5,2.5 H13.5 L15,4 H19 A2,2 0 0 1 21,6 V18 A2,2 0 0 1 19,20 H5 " +
                    "A2,2 0 0 1 3,18 V6 A2,2 0 0 1 5,4 Z M12,7.5 A4.5,4.5 0 1 0 12,16.5 " +
                    "A4.5,4.5 0 1 0 12,7.5 Z",
                regra = PathFillType.EvenOdd
            )
        }
    }

    /** Galeria para escolher a foto existente. */
    val Galeria: ImageVector by lazy {
        criar("Galeria") {
            preenchido(
                "M4,4 H20 A1,1 0 0 1 21,5 V19 A1,1 0 0 1 20,20 H4 A1,1 0 0 1 3,19 V5 " +
                    "A1,1 0 0 1 4,4 Z M5,6 V18 H19 V6 Z",
                regra = PathFillType.EvenOdd
            )
            preenchido("M6,17 L10,10.5 L13.5,15 L16,12 L19,17 Z")
            preenchido("M8.2,7.2 A1.5,1.5 0 1 0 8.2,10.2 A1.5,1.5 0 1 0 8.2,7.2 Z")
        }
    }

    /** Contatos (pessoa com livro/lista). */
    val Contatos: ImageVector by lazy {
        criar("Contatos") {
            preenchido("M12,3 A3.6,3.6 0 1 0 12,10.2 A3.6,3.6 0 1 0 12,3 Z")
            preenchido("M4.5,20 C4.5,16.4 7.9,13.2 12,13.2 C16.1,13.2 19.5,16.4 19.5,20 Z")
        }
    }

    /** Lâmpada: usada nas dicas do checklist. */
    val Dica: ImageVector by lazy {
        criar("Dica") {
            traco(
                "M12,3 A6,6 0 0 1 15.5,14 C15,14.6 14.7,15.2 14.6,16 H9.4 " +
                    "C9.3,15.2 9,14.6 8.5,14 A6,6 0 0 1 12,3 Z",
                1.8f
            )
            traco("M9.7,19 H14.3 M10.5,21 H13.5", 1.8f)
        }
    }

    val Menos: ImageVector by lazy {
        criar("Menos") {
            preenchido("M5,11 H19 A1,1 0 0 1 19,13 H5 A1,1 0 0 1 5,11 Z")
        }
    }

    // ------------------------------ utilidades ------------------------------

    private fun criar(
        nome: String,
        bloco: ImageVector.Builder.() -> Unit
    ): ImageVector = ImageVector.Builder(
        name = nome,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply(bloco).build()

    private fun ImageVector.Builder.preenchido(
        dados: String,
        regra: PathFillType = PathFillType.NonZero
    ) {
        addPath(
            pathData = addPathNodes(dados),
            pathFillType = regra,
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            strokeAlpha = 0f
        )
    }

    private fun ImageVector.Builder.traco(dados: String, espessura: Float) {
        addPath(
            pathData = addPathNodes(dados),
            fillAlpha = 0f,
            stroke = SolidColor(Color.Black),
            strokeAlpha = 1f,
            strokeLineWidth = espessura,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
    }
}
