# Banner Instructions
The banners folder is for image PNG files to display in web maps for every kingdom, camp, ruin, and sanctuary.
The file extension must be PNG, not JPG!
Any changes to image files requires a server restart!

Each kingdom in the server can have its own PNG file for its banner.
The name of the PNG file must match the kingdom name.
For example, if there is a kingdom in the server named Rome, then put a PNG file in this folder named Rome.png.
The image will appear in the label details of every town and the capital of Rome in the web map page.

When a kingdom changes its name, you must also change the name of the image PNG file.

If a kingdom in the server does not have a PNG file in this folder, then the default.png image is used.
You can replace the default.png file with any custom file you want.

Allowed image file names are:
- default.png           The default image used for all territories.
- camp.png              The banner image used for all barbarian camps.
- ruin.png              The banner image used for all ruins.
- sanctuary.png         The banner image used for all sanctuaries.
- <kingdom name>.png    The banner image used for a specific kingdom with a matching name.

## How to Enable Banners
To enable showing banner images, you must set the Konquest core.yml setting:
core.integration.map_options.show_banners: true

## Where to Place Banner Images
You must place your PNG files inside this banners folder, like:
<your server folder>/plugins/Konquest/banners/

## When Using Dynmap...
The images will be automatically copied into the Dynmap web images folder, like:
<your server folder>/plugins/dynmap/web/images/
You may want to double-check the files in Dynmap's directory to make sure they are correct.
Konquest does not delete files, so when kingdoms are renamed, the old images will stay there.
