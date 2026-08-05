-- Las sesiones son efimeras. Se revocan al adoptar tokens almacenados como SHA-256
-- para que ningun bearer previamente persistido pueda reutilizarse desde la base.
DELETE FROM sesion_usuario;
