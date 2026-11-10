# Regras de ofuscação do app.
# Room e Compose já trazem as regras necessárias via consumer rules.
# Nenhuma biblioteca de terceiros além do AndroidX é utilizada.

# Mantém nomes das classes de entidade do Room (reflexão em runtime)
-keep class br.com.mochiladeemergencia.app.data.local.entity.** { *; }
-keep class br.com.mochiladeemergencia.app.data.local.AppDatabase_Impl { *; }
