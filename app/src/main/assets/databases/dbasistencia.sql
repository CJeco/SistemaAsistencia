BEGIN TRANSACTION;
CREATE TABLE IF NOT EXISTS "asistencia_docentes" (
	"id_asistencia"	INTEGER,
	"docente_id"	INTEGER NOT NULL,
	"fecha"	TEXT NOT NULL,
	"hora_entrada"	TEXT NOT NULL,
	"hora_salida"	TEXT,
	"minutos_tardanza"	INTEGER DEFAULT 0,
	"estado"	TEXT NOT NULL,
	"observacion"	TEXT,
	PRIMARY KEY("id_asistencia" AUTOINCREMENT),
	FOREIGN KEY("docente_id") REFERENCES "docentes"("id") on DELETE CASCADE
),
CREATE TABLE IF NOT EXISTS "docentes" (
	"id"	INTEGER,
	"dni"	TEXT NOT NULL UNIQUE,
	"nombres"	TEXT NOT NULL,
	"apellidos"	TEXT NOT NULL,
	"correo"	TEXT,
	"telefono"	TEXT,
	"horario_entrada"	TEXT NOT NULL DEFAULT '07:00:00',
	"activo"	INTEGER DEFAULT 1,
	PRIMARY KEY("id" AUTOINCREMENT)
);
CREATE TABLE IF NOT EXISTS "licencia_permisos" (
	"id_permisos"	INTEGER,
	"docente_id"	INTEGER NOT NULL,
	"fecha_inicio"	TEXT NOT NULL,
	"fecha_fin"	TEXT NOT NULL,
	"tipo"	TEXT NOT NULL,
	"motivo"	TEXT,
	PRIMARY KEY("id_permisos" AUTOINCREMENT),
	FOREIGN KEY("docente_id") REFERENCES "docentes"("id") ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS "idx_asistencia_docentes" ON "asistencia_docentes" (
	"docente_id"
);
CREATE INDEX IF NOT EXISTS "idx_asistencia_fecha" ON "asistencia_docentes" (
	"fecha"
);
CREATE INDEX IF NOT EXISTS "idx_docente_dni" ON "docentes" (
	"dni"
);
COMMIT;
