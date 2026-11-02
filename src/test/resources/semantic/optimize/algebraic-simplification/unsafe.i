routine f() : integer is
    return 3
end
routine t(n : integer, r : real, a : array [3] integer) is
    print n + 0.0, n * 1.0, n / 1.0
    print r + 0.0, r * 0.0, r - -0.0
    print f() + 0, f() * 0, (f() + n) * 1
    print (10 / n) * 0, (n + 1) * 0, a[n] * 0
    print 0.0 + n, 1.0 * n, 0.0 * n
    print 0.0 + r, 0.0 * r
    print 0 + f(), 1 * f(), 0 * f(), 1 * (f() + n)
    print 0 * (10 / n), 0 * (n + 1), 0 * a[n]
    print 0 - n, 1 / n
end
