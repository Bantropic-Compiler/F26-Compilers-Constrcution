type R is record
    var a: array [2] array [3] integer
end
routine p(r: R) is
    print r.a[3][4]
end
