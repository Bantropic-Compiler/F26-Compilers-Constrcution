routine t(n : integer, r : real) is
    print n + 0, n - 0, n * 1, n / 1, n * 0
    print r - 0.0, r * 1.0, r / 1.0
    print 0 + n, 1 * n, 0 * n, 1.0 * r
    print 1 * (0 + n), 0 * (1 * n)
end
