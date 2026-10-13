"""Hand-authored dish silhouettes; shared materials, never a generic roast fallback."""
def elements_for(name, box):
    e=[]
    def b(x,y,z,w,h,d,t,label): e.append(box([x,y,z],[x+w,y+h,z+d],t,label))
    def plate(): b(1,1,2,14,1,12,1,'Plate')
    def board(): b(1,1,2,14,1,12,0,'Serving board')
    def chunks(points,t,size=2):
        for x,y,z in points: b(x,y,z,size,1,size,t,'Ingredient pieces')
    def bowl(t,style='round'):
        if style=='cup':
            b(5,1,5,6,2,6,1,'Cup foot'); b(4,3,4,8,6,8,1,'Cup'); b(5,9,5,6,.4,6,t,'Soup')
            b(12,4,6,2,1,4,0,'Handle'); b(13,5,6,1,3,4,0,'Handle'); b(12,8,6,2,1,4,0,'Handle')
            return 9.4
        if style=='pot':
            b(4,1,4,8,2,8,11,'Pot foot'); b(3,3,3,10,5,10,11,'Cooking pot'); b(4,8,4,8,.4,8,t,'Contents')
            b(1,6,6,2,2,4,11,'Left handle'); b(13,6,6,2,2,4,11,'Right handle')
            return 8.4
        if style=='boat':
            b(2,2,5,12,2,6,0,'Boat bowl'); b(1,4,5,2,3,6,0,'Bow'); b(13,4,5,2,3,6,0,'Stern')
            b(3,4,4,10,2,1,0,'Rim'); b(3,4,11,10,2,1,0,'Rim'); b(3,4,5,10,1,6,t,'Contents')
            return 5
        # Stepped octagonal bowl, with visible inset food and rim.
        b(5,1,5,6,1,6,1,'Bowl foot'); b(4,2,4,8,2,8,1,'Bowl base')
        b(3,4,3,10,2,10,t,'Contents')
        for x,z,w,d in [(2,4,1,8),(13,4,1,8),(4,2,8,1),(4,13,8,1),(3,3,1,1),(12,3,1,1),(3,12,1,1),(12,12,1,1)]: b(x,4,z,w,3,d,1,'Ceramic rim')
        return 6
    def fish(x,y,z,t=9):
        b(x,y,z,7,2,3,t,'Fish fillet'); b(x+7,y,z-1,2,2,5,t,'Tail'); b(x+1,y+2,z,1,.3,3,11,'Grill mark'); b(x+4,y+2,z,1,.3,3,11,'Grill mark')
    def loaf(x,y,z,w=10):
        b(x,y,z,w,3,5,2,'Bread'); b(x+1,y+3,z+1,w-2,1,3,2,'Rounded crust')
    def mushroom(x,y,z,t=15):
        b(x+1,y,z+1,1,2,1,10,'Mushroom stem'); b(x,y+2,z,3,1,3,t,'Mushroom cap')
    def leg(x,y,z):
        b(x,y,z,5,4,5,3,'Roasted meat'); b(x+1,y+4,z+1,3,1,3,2,'Crisp skin'); b(x+5,y+1,z+2,3,1,1,10,'Bone')
    def sandwich(x,y,z):
        b(x,y,z,5,1,5,2,'Bread'); b(x,y+1,z,5,1,5,6,'Lettuce'); b(x,y+2,z,5,1,5,5,'Cheese'); b(x,y+3,z,5,1,5,2,'Bread')
    if name=='hunters_stew':
        h=bowl(3,'pot'); chunks([(4,h,5),(8,h,8)],3,3); b(6,h,6,1,4,1,10,'Upright bone'); chunks([(9,h,4)],6)
    elif name=='farmhouse_roast':
        plate(); leg(3,2,5); chunks([(10,2,4),(11,2,10)],5,3); b(3,2,11,5,1,1,6,'Green beans')
    elif name=='spiced_steak':
        board(); b(3,2,4,9,2,7,3,'Steak'); b(4,4,5,7,.5,5,4,'Spice crust')
        for x in (5,8,11): b(x,4.5,5,.5,.2,5,11,'Sear mark')
        b(12,2,6,1,2,5,4,'Whole pepper')
    elif name in ('tropical_skewer','jungle_pepper_skewer'):
        board()
        for z in ([5,10] if name=='jungle_pepper_skewer' else [7]):
            b(1,4,z,14,.5,.5,0,'Skewer')
            for i,t in enumerate([3,8,5,6] if name=='jungle_pepper_skewer' else [5,3,4,5]): b(2+i*3,3,z-1,2,3,3,t,'Skewered piece')
    elif name=='bog_gumbo':
        h=bowl(6,'pot'); mushroom(4,h,4); b(8,h,8,4,1,2,3,'Sausage'); b(5,h,10,1,2,3,6,'Okra')
    elif name=='miners_meal':
        board(); loaf(2,2,3,6); b(3,3,2,1,5,1,11,'Knife blade'); b(3,8,2,1,3,1,0,'Knife handle'); mushroom(10,2,8); chunks([(3,2,10)],5,3)
    elif name=='winter_hotpot':
        h=bowl(7,'pot'); b(3,h,4,9,1,2,10,'Pork slice'); b(4,h+1,8,7,1,2,4,'Pork slice'); b(10,h,10,2,3,2,6,'Leek')
    elif name=='seafood_rice':
        plate(); b(3,2,4,9,2,8,7,'Rice bed'); fish(3,4,6); chunks([(5,4,10),(9,4,4)],6)
    elif name=='mushroom_risotto':
        h=bowl(7); mushroom(4,h,4); mushroom(8,h,8,3); mushroom(9,h,4)
    elif name=='nether_chili':
        h=bowl(4,'pot'); chunks([(4,h,4),(8,h,7),(5,h,10)],3); b(10,h,5,1,4,1,12,'Hot pepper'); b(4,h,8,1,3,1,4,'Pepper')
    elif name=='ender_salad':
        h=bowl(6); chunks([(4,h,4),(8,h,8),(9,h,4)],8,3); b(6,h,9,1,4,3,6,'Leaf')
    elif name=='garlic_bread':
        board()
        for x in (2,6,10): loaf(x,2,5,3); b(x,6,6,3,.5,3,5,'Garlic butter'); chunks([(x+1,6.5,7)],6,1)
    elif name=='cheesy_potato':
        plate(); b(3,2,5,10,3,6,2,'Baked potato'); b(4,5,6,8,1,4,7,'Split fluffy center'); b(5,6,6,6,1,4,5,'Melted cheese'); b(9,5,5,2,2,1,5,'Cheese drip')
    elif name=='vegetable_pizza':
        plate(); b(2,2,4,12,1,8,2,'Pizza crust'); b(4,2,2,8,1,12,2,'Octagonal crust'); b(3,3,4,10,.5,8,5,'Cheese'); b(4,3,3,8,.5,10,5,'Cheese')
        chunks([(4,3.5,5),(9,3.5,8),(5,3.5,10)],4); chunks([(8,3.5,4),(3,3.5,8)],6)
    elif name=='cornbread':
        board()
        for x,z in [(3,4),(9,4),(6,10)]: b(x,2,z,4,3,4,2,'Cornbread piece'); b(x,5,z,4,.5,4,5,'Golden top')
        b(4,5.5,5,2,1,2,5,'Butter pat')
    elif name in ('farmers_lunch','builders_lunch','explorers_meal'):
        board(); sandwich(2,2,4)
        if name=='farmers_lunch': b(10,2,4,2,2,8,14,'Corn cob'); b(9,2,11,4,1,2,6,'Corn husk')
        elif name=='builders_lunch': sandwich(8,3,7); b(9,2,3,4,1,3,3,'Meat side')
        else: b(9,2,3,4,5,4,0,'Travel cup'); b(9.5,7,3.5,3,.3,3,3,'Drink'); b(9,2,10,4,2,3,8,'Dried fruit')
    elif name=='fishermans_stew':
        h=bowl(4,'boat'); fish(3,h,7); b(5,h,5,1,3,1,6,'Herb')
    elif name=='night_watch_stew':
        h=bowl(3,'cup'); mushroom(5,h,5); b(9,h,8,1,3,1,14,'Golden carrot')
    elif name in ('berry_pie','chorus_pie','honey_tart'):
        plate(); t={'berry_pie':4,'chorus_pie':8,'honey_tart':14}[name]
        b(3,2,4,10,2,8,2,'Pastry crust'); b(4,2,3,8,2,10,2,'Pastry edges'); b(4,4,4,8,1,8,t,'Filling')
        if name=='berry_pie':
            for x in (4,7,10): b(x,5,4,1,.5,8,2,'Lattice')
            for z in (5,9): b(4,5.5,z,8,.5,1,2,'Cross lattice')
        elif name=='chorus_pie': b(6,5,6,4,2,4,8,'Chorus fruit'); b(7,7,7,2,2,2,1,'Cream')
        else: b(6,5,6,4,1,4,5,'Custard'); b(7,6,7,2,2,2,1,'Cream swirl')
    elif name=='chocolate_cake':
        plate(); b(4,2,4,8,3,8,3,'Lower chocolate layer'); b(4,5,4,8,1,8,1,'Cream filling'); b(4,6,4,8,3,8,3,'Upper chocolate layer'); b(4,9,4,8,1,8,3,'Icing'); chunks([(5,10,5),(9,10,9)],4)
    elif name=='captains_chowder':
        h=bowl(7,'cup'); fish(4,h,6); b(6,h,9,2,2,2,10,'Clam')
    elif name=='frost_garlic_soup':
        h=bowl(1); b(6,h,6,4,3,4,10,'Garlic bulb'); b(7,h+3,7,2,2,2,6,'Garlic stem'); chunks([(4,h,9),(10,h,5)],13)
    elif name=='marsh_rice_bowl':
        h=bowl(7); b(3,h,6,10,1,1,0,'Chopstick'); b(3,h+1,9,10,1,1,0,'Chopstick'); mushroom(5,h,3,3)
    elif name=='ancient_sap_pudding':
        plate(); b(4,2,4,8,2,8,14,'Sap sauce'); b(5,4,5,6,4,6,5,'Pudding'); b(6,8,6,4,1,4,14,'Caramel cap'); b(8,9,7,4,.5,2,6,'Leaf')
    elif name=='prime_hunters_roast':
        board(); leg(2,2,3); leg(6,3,9); b(10,2,3,3,2,3,5,'Roast potato')
    elif name=='restorative_broth':
        h=bowl(6,'cup'); b(7,h,7,2,4,2,10,'Marrow bone'); b(6,h+4,6,4,1,4,10,'Bone joint'); chunks([(5,h,5)],4)
    elif name=='tyrants_stock':
        h=bowl(3,'pot'); b(5,h,5,6,2,6,10,'Fossil vertebra'); b(7,h+2,7,2,.5,2,3,'Marrow'); b(6,h,4,1,4,1,10,'Rib'); b(10,h,9,1,5,1,10,'Rib')
    elif name=='storm_omelette':
        plate(); b(3,2,4,10,2,7,5,'Folded egg'); b(4,4,5,8,1,5,14,'Golden fold'); b(6,5,6,2,1,2,1,'Cream'); b(9,5,7,1,3,1,9,'Charged garnish')
    elif name=='titan_curry':
        h=bowl(12,'boat'); b(4,h,6,4,1,4,6,'Serpent steak'); b(5,h+1,7,2,.3,2,4,'Steak center'); b(10,h,6,2,2,4,7,'Rice mound')
    elif name=='witchs_gumbo':
        h=bowl(15,'pot'); mushroom(4,h,5,8); b(10,h,8,1,5,1,0,'Stirring spoon'); b(9,h+4,7,3,2,3,0,'Spoon head'); chunks([(8,h,4)],12)
    elif name=='mountain_bisque':
        h=bowl(5,'cup'); b(6,h,6,2,4,2,10,'Salt crystal'); b(9,h,8,1,2,1,10,'Salt crystal'); b(5,h,9,3,1,1,6,'Herb')
    elif name=='hot_wyrm_stew':
        h=bowl(12,'pot'); b(4,h,4,7,1,3,5,'Wyrm strip'); b(5,h+1,5,6,1,1,13,'Frosted skin'); b(6,h,9,5,2,2,5,'Fat strip')
    elif name=='kraken_platter':
        plate()
        for x in (3,7,11):
            b(x,2,4,2,2,7,8,'Tentacle'); b(x,4,9,2,3,2,8,'Tentacle curl'); chunks([(x,4,5),(x,4,7)],1,1)
    elif name=='cavern_stew':
        h=bowl(3); mushroom(4,h,5,11); mushroom(8,h,8,11); b(9,h,4,2,2,2,3,'Truffle')
    elif name=='shadow_stew':
        h=bowl(15,'boat'); b(5,h,6,6,1,4,9,'Royal jelly'); b(6,h+1,7,4,1,2,14,'Crown garnish')
        for x in (6,9): b(x,h+2,7,1,2,2,14,'Crown tip')
    elif name=='sovereign_risotto':
        h=bowl(7); b(6,h,6,3,3,3,10,'Sovereign stem'); b(3,h+3,3,9,2,9,8,'Sovereign cap'); chunks([(5,h+5,5),(9,h+5,8)],1)
    elif name=='infernal_roast':
        board(); b(3,2,4,10,4,8,11,'Charred roast'); b(4,6,5,8,1,6,3,'Meat'); b(5,7,6,1,2,4,12,'Fiery split'); b(9,7,6,1,3,4,12,'Fiery split')
    elif name=='soul_broth':
        h=bowl(9,'cup'); b(7,h,7,2,2,2,13,'Soul foam'); b(7,h+3,7,1,1,1,9,'Suspended soul mote'); b(10,h+1,8,1,1,1,9,'Soul mote')
    elif name=='void_delight':
        plate(); b(5,2,5,6,1,6,15,'Dessert base'); b(6,3,6,4,1,4,1,'Cream'); b(5,6,5,6,3,6,8,'Floating confection'); b(6,9,6,4,1,4,1,'Icing'); b(7,11,7,2,2,2,9,'Suspended fruit')
    else: raise ValueError('Missing authored meal: '+name)
    return e
