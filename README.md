# TunisPromo
TunisPromo est une application Android en Java qui regroupe les promotions de sites e-commerce tunisiens (Mytek, Fatales, etc.) sur un seul écran.

L’utilisateur peut s’inscrire et se connecter (Firebase). Il consulte une liste de promos (titre, prix, réduction, image) chargée depuis un serveur Flask via Retrofit. Il ouvre le détail d’une offre, la consulte sur le site web dans le navigateur, et peut l’ajouter aux favoris (Firestore).

Un administrateur dispose d’un tableau de bord pour forcer le rechargement des promos (scraping côté serveur). L’app couvre les notions du cours : activités, Intents, RecyclerView, menus, Toast, AlertDialog, et connexion à une base Firebase.

En une phrase : application mobile de catalogue de promotions tunisiennes, avec comptes utilisateurs, favoris cloud et mise à jour des offres par un admin.
