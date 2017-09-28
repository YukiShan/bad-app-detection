# import MySQLdb
import sqlite3
import datetime
__author__ = 'Shanshan'

db_name='Crawler_apple_pub'


def save_itunes_categories(conn):
    print('Start to load categories')
    f= open("./itunes_categories.txt")
    cates=[]
    for line in f:
        eles=line.split('=>')
        code=eles[0].strip()
        subcate=eles[1].strip()
        category=subcate
        if category[0:2]=='|-' and len(cates)>0: # it is the subcategory
            category=cates[-1][3]
            subcate=subcate[2:]
        cates.append((code,'itunes','categories',category,subcate))
    cur=conn.cursor()


    now=str(datetime.datetime.now())
    for cate in cates:
        sql='INSERT INTO StoreParameters (code,app_store,type,attr1,attr2,date) values("'+\
            cate[0]+'","'+cate[1]+'","'+cate[2]+'","'+cate[3]+'","'+cate[4]+'","%s")'%now
        cur.execute(sql)
    cur.close()
    f.close()
    print('End of loading categories')
    return

def save_itunes_storefront(conn):
    print('Start to load storefront')
    f=open("./itunes_storefront.txt")
    cates=[]
    for line in f:
        eles=line.split('=>')
        country_code=eles[0].strip()
        storefront=eles[1].strip()
        country_name=eles[2].strip()
        cates.append((storefront,'itunes','storefronts',country_code,country_name))
    cur=conn.cursor()
    for cate in cates:
        sql='INSERT INTO StoreParameters (code,app_store,type,attr1,attr2,date) values("'+ \
            cate[0]+'","'+cate[1]+'","'+cate[2]+'","'+cate[3]+'","'+cate[4]+'","%s")'%datetime.datetime.now()
        cur.execute(sql)
    cur.close()
    print('End of loading storefront')
    return

def init_database(type=True, sqlfile_path='/Users/ssli/Documents/Research/testDB.db'):
    if type:
        conn = sqlite3.connect(sqlfile_path.strip())
    else:
        import MySQLdb
        conn=MySQLdb.connect (host ='130.203.32.48' , user = 'root', passwd = '', db =db_name)
    save_itunes_categories(conn)
    save_itunes_storefront(conn)



if __name__=='__main__':
    import sys
    if len(sys.argv)<2:
        print 'Usage: ptyhon load_itunes.py {sqldb_path}'

    init_database(sqlfile_path=sys.argv[1])