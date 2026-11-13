# Importación integral de socios y cuotas históricas (propuesta)

## Alcance
Se admiten dos CSV de UTF-8, de hasta 1 MB: la plantilla integral que descarga Angular y el control de cuotas con títulos previos ("Nombre y apellido", "N° de socio", "DNI", enero a diciembre). El año se selecciona en la interfaz. La vista previa no escribe registros. La confirmación se ejecuta en una sola transacción para una institución.

## Estados
- **Pagó**: declaración histórica de cuota pagada, sin importe ni fecha precisa conocidos.
- **No pagó**: estado histórico pendiente, no se registra como deuda exigible por importe.
- Celda vacía: sin información, no genera estado.
- Los registros se guardan en `historial_cuota_importada`, separados de `cuota` y `pago`. No aparecen como ingresos contables ni cobranza conciliada. Ningún sistema de pagos se activa con esta función.

## Identidades
DNI identifica al socio dentro de su institución. Los DNI existentes conservan sus datos; diferencias en nombre/apellido se informan como conflicto. Los nuevos socios requieren nombre y apellido diferenciados (la importación del control con nombre único solo admite nombres de exactamente dos términos; el resto debe corregirse en la plantilla integral). Las filas ambiguas bloquean todo el archivo.

## Idempotencia
Repetir el CSV omite estados históricos iguales; estados históricos contradictorios y números de socio asociados a otro DNI bloquean la operación. La confirmación requiere vista previa válida.

## Límites
El historial separado **no cambia** el estado de las cuotas financieras existentes: para visualizarlo como parte del estado de cuenta se necesitará extender la consulta de cuotas combinando ambos orígenes, distinguiendo siempre las declaraciones históricas de los pagos reales. Los totales presentes en el control no se usan para inferir estados faltantes. Se debe validar cualquier fuente antes de importar a producción.

## Pruebas y despliegue
Validar con datos sintéticos primero, revisar migración Flyway V12 en MySQL 8.4 aislado y ejecutar CI. No subir el control real de Retoños de Abril ni archivos con DNI a GitHub. No hacer merge/despliegue antes de verificar el pipeline.
